package com.ysh.serverhelper.notifier;

import com.google.gson.JsonObject;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.ws.QQWSClient;

public class QQNotifier implements Notifier {
    private final QQWSClient wsClient;
    private final ModConfig.QQConfig config;

    public QQNotifier(ModConfig.QQConfig config, QQWSClient wsClient) {
        this.config = config;
        this.wsClient = wsClient;
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
        JsonObject params = new JsonObject();
        params.addProperty("group_id", config.getGroupId());
        params.addProperty("message", message);
        wsClient.sendAction("send_group_msg", params);
    }
}
