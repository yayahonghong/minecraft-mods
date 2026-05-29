package com.ysh.serverhelper.qqcmd;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import net.minecraft.server.MinecraftServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class QQCommandWSClient {
    private final HttpClient client;
    private volatile WebSocket webSocket;
    private volatile boolean active;
    private String wsUrl;
    private ModConfig.QQConfig config;

    public QQCommandWSClient() {
        this.client = HttpClient.newBuilder().build();
    }

    public void connect(ModConfig.QQConfig qqConfig, MinecraftServer mcServer) {
        this.config = qqConfig;
        QQCommandHandler.init(mcServer);

        String apiUrl = qqConfig.getApiUrl().replaceAll("/+$", "");
        this.wsUrl = (apiUrl.startsWith("https://")
                ? "wss://" + apiUrl.substring(8)
                : "ws://" + apiUrl.substring(7)) + "/ws";

        if (!qqConfig.getToken().isEmpty()) {
            wsUrl += "?access_token=" + qqConfig.getToken();
        }

        active = true;
        connectInternal();
    }

    private void connectInternal() {
        if (!active) return;
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WsListener())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    ServerHelperMod.LOGGER.info("QQ WebSocket connected to {}", wsUrl);
                })
                .exceptionally(e -> {
                    ServerHelperMod.LOGGER.warn("QQ WS connect failed, retry in 5s: {}", e.getMessage());
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
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Server stopping");
            webSocket = null;
        }
    }

    private class WsListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            QQCommandHandler.handle(data.toString(), config);
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            ServerHelperMod.LOGGER.warn("QQ WS error: {}", error.getMessage());
            webSocket = null;
            scheduleReconnect();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            ServerHelperMod.LOGGER.warn("QQ WS closed ({}/{}), reconnect in 5s", statusCode, reason);
            webSocket = null;
            scheduleReconnect();
            return WebSocket.Listener.super.onClose(ws, statusCode, reason);
        }
    }
}
