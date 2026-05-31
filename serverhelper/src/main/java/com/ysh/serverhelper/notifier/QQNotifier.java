package com.ysh.serverhelper.notifier;

import com.google.gson.JsonObject;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.ws.WSClient;

/**
 * NapCat通知实现
 */
public class QQNotifier implements Notifier {

    /**
     * WebSocket客户端
     */
    private final WSClient wsClient;

    /**
     * NapCat通知配置
     */
    private final ModConfig.QQConfig config;

    public QQNotifier(ModConfig.QQConfig config, WSClient wsClient) {
        this.config = config;
        this.wsClient = wsClient;
    }

    @Override
    public String getName() {
        return "QQ(NapCat)";
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled() && !config.getApiUrl().isEmpty() && config.getGroupId() > 0;
    }

    @Override
    public void send(String message) {
        if (!isEnabled()) return;
        JsonObject params = new JsonObject();
        params.addProperty("group_id", config.getGroupId());
        params.addProperty("message", message);
        wsClient.sendAction("send_group_msg", params);
    }
}
