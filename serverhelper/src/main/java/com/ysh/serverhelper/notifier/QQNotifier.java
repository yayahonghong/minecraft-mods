package com.ysh.serverhelper.notifier;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class QQNotifier implements Notifier {
    private final HttpClient client;
    private final ModConfig.QQConfig config;

    public QQNotifier(ModConfig.QQConfig config) {
        this.client = HttpClient.newHttpClient();
        this.config = config;
    }

    @Override
    public String getName() { return "QQ(NapCat)"; }

    @Override
    public boolean isEnabled() {
        return config.isEnabled() && !config.getApiUrl().isEmpty() && config.getGroupId() > 0;
    }

    @Override
    public void send(JoinNotification notification, String message) {
        if (!isEnabled()) return;
        String json = "{\"group_id\":" + config.getGroupId() + ",\"message\":\"" + escapeJson(message) + "\"}";
        String url = config.getApiUrl().replaceAll("/+$", "") + "/send_group_msg";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8");
        if (!config.getToken().isEmpty()) {
            builder.header("Authorization", "Bearer " + config.getToken());
        }

        client.sendAsync(
            builder.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    ServerHelperMod.LOGGER.warn("NapCat returned {}: {}", response.statusCode(), response.body());
            })
            .exceptionally(e -> { ServerHelperMod.LOGGER.warn("NapCat failed", e); return null; });
    }

    private static String escapeJson(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
