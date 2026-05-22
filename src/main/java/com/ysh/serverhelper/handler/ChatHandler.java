package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatHandler {
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (!(sender instanceof ServerPlayer player)) return;
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var ec = config.getEvents().get("chat");
            if (ec == null || !ec.isEnabled()) return;
            if (config.getExcludedPlayers().contains(player.getName().getString())) return;

            String msg = ec.getMessage()
                    .replace("{player}", player.getName().getString())
                    .replace("{message}", message.signedContent())
                    .replace("{time}", LocalDateTime.now().format(TF));

            var n = new QQNotifier(config.getQq());
            if (n.isEnabled()) Thread.ofVirtual().start(() -> n.send(null, msg));
        });
    }
}
