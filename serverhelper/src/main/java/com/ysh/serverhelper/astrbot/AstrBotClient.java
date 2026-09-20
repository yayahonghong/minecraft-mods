package com.ysh.serverhelper.astrbot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * AstrBot HTTP 客户端。
 * 承担两类调用：
 * 1. AstrBot OpenAPI（base_url + api_key）：主动发送 IM 消息。
 * 2. mc_bridge 插件桥（bridge_url + internal_token）：轮询命令、回传执行结果。
 */
public class AstrBotClient {
    private static final Gson GSON = new Gson();
    private static volatile SSLContext insecureSslContext;

    private final HttpClient httpClient;
    private volatile ModConfig.AstrBotConfig config;

    public AstrBotClient(ModConfig.AstrBotConfig config) {
        this.config = config;
        this.httpClient = buildHttpClient(config.isInsecureTls());
    }

    public void updateConfig(ModConfig.AstrBotConfig config) {
        this.config = config;
    }

    private static HttpClient buildHttpClient(boolean insecureTls) {
        var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
        if (insecureTls) {
            builder.sslContext(insecureSslContext());
        }
        return builder.build();
    }

    private static SSLContext insecureSslContext() {
        SSLContext ctx = insecureSslContext;
        if (ctx == null) {
            try {
                ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[]{new X509ExtendedTrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }}, new SecureRandom());
                insecureSslContext = ctx;
            } catch (Exception e) {
                throw new IllegalStateException("初始化跳过证书校验的 SSLContext 失败", e);
            }
        }
        return ctx;
    }

    private static String trimTrailingSlash(String url) {
        return url.replaceAll("/+$", "");
    }

    /**
     * 通过 AstrBot OpenAPI 主动向 UMO（会话）发送消息。
     * POST /api/v1/im/messages  {"umo": "...", "message": "..."}
     */
    public CompletableFuture<Void> sendImMessage(String text) {
        ModConfig.AstrBotConfig cfg = this.config;
        String baseUrl = trimTrailingSlash(cfg.getBaseUrl());
        String umo = cfg.getUmo();
        if (baseUrl.isEmpty() || cfg.getApiKey().isEmpty() || umo.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        JsonObject body = new JsonObject();
        body.addProperty("umo", umo);
        body.addProperty("message", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/im/messages"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cfg.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(resp -> {
                    if (resp.statusCode() != 200 || !isStatusOk(resp.body())) {
                        ServerHelperMod.LOGGER.warn("AstrBot 发送消息失败: HTTP {} {}", resp.statusCode(), resp.body());
                    }
                })
                .exceptionally(e -> {
                    ServerHelperMod.LOGGER.warn("AstrBot 发送消息异常: {}", e.getMessage());
                    return null;
                });
    }

    /**
     * 长轮询 mc_bridge 插件命令队列。
     * GET /mc/poll?token=...&wait=秒
     * 返回命令数组（可能为空），每个元素包含 id/umo/user_id/text。
     */
    public JsonArray pollCommands(int waitSeconds) throws Exception {
        ModConfig.AstrBotConfig cfg = this.config;
        String bridgeUrl = trimTrailingSlash(cfg.getBridgeUrl());
        if (bridgeUrl.isEmpty()) return new JsonArray();

        String url = bridgeUrl + "/mc/poll?token="
                + URLEncoder.encode(cfg.getInternalToken(), StandardCharsets.UTF_8)
                + "&wait=" + waitSeconds;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(waitSeconds + 15L))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("poll HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonElement root = JsonParser.parseString(resp.body());
        JsonArray commands = root.getAsJsonObject().has("commands")
                ? root.getAsJsonObject().getAsJsonArray("commands")
                : new JsonArray();
        return commands;
    }

    /**
     * 将命令执行结果回传给 mc_bridge 插件，由插件回复到 QQ 群。
     * POST /mc/reply  {"token": "...", "id": "...", "text": "..."}
     */
    public void replyCommand(String id, String text) {
        try {
            ModConfig.AstrBotConfig cfg = this.config;
            String bridgeUrl = trimTrailingSlash(cfg.getBridgeUrl());
            if (bridgeUrl.isEmpty()) return;

            JsonObject body = new JsonObject();
            body.addProperty("token", cfg.getInternalToken());
            body.addProperty("id", id);
            body.addProperty("text", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bridgeUrl + "/mc/reply"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                ServerHelperMod.LOGGER.warn("AstrBot 命令回传失败: HTTP {} {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            ServerHelperMod.LOGGER.warn("AstrBot 命令回传异常: {}", e.getMessage());
        }
    }

    private static boolean isStatusOk(String body) {
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            return "ok".equals(obj.get("status").getAsString());
        } catch (Exception e) {
            return false;
        }
    }
}
