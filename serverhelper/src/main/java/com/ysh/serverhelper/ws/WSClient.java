package com.ysh.serverhelper.ws;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * NapCat WebSocket客户端
 */
public class WSClient {
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final AtomicLong echoCounter = new AtomicLong(0);
    private final ConcurrentHashMap<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

    private volatile WebSocket webSocket;
    private volatile boolean active;
    private String wsUrl;
    private ModConfig.QQConfig config;
    private Consumer<String> eventListener;

    public void connect(ModConfig.QQConfig qqConfig) {
        this.config = qqConfig;

        String apiUrl = qqConfig.getApiUrl().replaceAll("/+$", "");
        this.wsUrl = (apiUrl.startsWith("https://")
                ? "wss://" + apiUrl.substring(8)
                : "ws://" + apiUrl.substring(7));

        active = true;

        var wsBuilder = httpClient.newWebSocketBuilder();
        if (!config.getToken().isEmpty()) {
            wsBuilder.header("Authorization", "Bearer " + config.getToken());
        }
        try {
            this.webSocket = wsBuilder.buildAsync(URI.create(wsUrl), new WsListener())
                    .get(10, TimeUnit.SECONDS);
            ServerHelperMod.LOGGER.info("QQ WebSocket connected to {}", wsUrl);
        } catch (Exception e) {
            ServerHelperMod.LOGGER.warn("QQ WS connect failed: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void connectInternal() {
        if (!active) return;
        var wsBuilder = httpClient.newWebSocketBuilder();
        if (!config.getToken().isEmpty()) {
            wsBuilder.header("Authorization", "Bearer " + config.getToken());
        }
        wsBuilder.buildAsync(URI.create(wsUrl), new WsListener())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    ServerHelperMod.LOGGER.info("QQ WebSocket reconnected to {}", wsUrl);
                })
                .exceptionally(e -> {
                    ServerHelperMod.LOGGER.warn("QQ WS reconnect failed: {}", e.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (!active) return;
        Thread.startVirtualThread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) { return; }
            if (active) connectInternal();
        });
    }

    public void disconnect() {
        active = false;
        for (var entry : pendingRequests.entrySet()) {
            entry.getValue().completeExceptionally(new RuntimeException("WS disconnected"));
        }
        pendingRequests.clear();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Server stopping");
            webSocket = null;
        }
    }

    public CompletableFuture<JsonObject> sendAction(String action, JsonObject params) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        WebSocket ws = this.webSocket;
        if (ws == null || !active) {
            future.completeExceptionally(new RuntimeException("WS not connected"));
            return future;
        }

        String echo = "req_" + echoCounter.incrementAndGet();
        pendingRequests.put(echo, future);

        JsonObject payload = new JsonObject();
        payload.addProperty("action", action);
        payload.add("params", params);
        payload.addProperty("echo", echo);

        ws.sendText(GSON.toJson(payload), true)
                .exceptionally(e -> {
                    CompletableFuture<JsonObject> f = pendingRequests.remove(echo);
                    if (f != null) f.completeExceptionally(e);
                    return null;
                });

        Thread.startVirtualThread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) { }
            CompletableFuture<JsonObject> f = pendingRequests.remove(echo);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(new TimeoutException("WS action timeout: " + action));
            }
        });

        return future;
    }

    public void setEventListener(Consumer<String> listener) {
        this.eventListener = listener;
    }

    private class WsListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            String text = data.toString();
            try {
                JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
                if (obj.has("echo") && !obj.get("echo").isJsonNull()) {
                    String echo = obj.get("echo").getAsString();
                    CompletableFuture<JsonObject> future = pendingRequests.remove(echo);
                    if (future != null) {
                        future.complete(obj);
                    }
                } else if (eventListener != null) {
                    eventListener.accept(text);
                }
            } catch (Exception e) {
                ServerHelperMod.LOGGER.warn("QQ WS parse error", e);
            }
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            ServerHelperMod.LOGGER.warn("QQ WS error: {}", error.getMessage());
            webSocket = null;
            for (var entry : pendingRequests.entrySet()) {
                entry.getValue().completeExceptionally(error);
            }
            pendingRequests.clear();
            scheduleReconnect();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            ServerHelperMod.LOGGER.warn("QQ WS closed ({}/{}), reconnect in 5s", statusCode, reason);
            webSocket = null;
            for (var entry : pendingRequests.entrySet()) {
                entry.getValue().completeExceptionally(new RuntimeException("WS closed"));
            }
            pendingRequests.clear();
            scheduleReconnect();
            return WebSocket.Listener.super.onClose(ws, statusCode, reason);
        }
    }
}
