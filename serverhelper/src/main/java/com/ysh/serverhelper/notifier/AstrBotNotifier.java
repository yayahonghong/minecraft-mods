package com.ysh.serverhelper.notifier;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;

import java.util.concurrent.CompletableFuture;

/**
 * AstrBot 通知实现：通过 OpenAPI 的 IM 主动消息接口发送。
 */
public class AstrBotNotifier implements Notifier {

    private final ModConfig.AstrBotConfig config;

    public AstrBotNotifier(ModConfig.AstrBotConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "QQ(AstrBot)";
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled()
                && !config.getBaseUrl().isEmpty()
                && !config.getApiKey().isEmpty()
                && !config.getUmo().isEmpty();
    }

    @Override
    public CompletableFuture<Void> send(String message) {
        if (!isEnabled()) return CompletableFuture.completedFuture(null);
        var client = ServerHelperMod.astrBotClient;
        if (client == null) return CompletableFuture.completedFuture(null);
        return client.sendImMessage(message);
    }
}
