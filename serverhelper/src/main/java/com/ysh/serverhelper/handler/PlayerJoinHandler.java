package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerJoinHandler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var eventConfig = config.getEvents().get("join");
            if (eventConfig == null || !eventConfig.isEnabled()) return;
            if (config.getExcludedPlayers().contains(player.getName().getString())) return;

            String ip = "";
            if (handler.getRemoteAddress() instanceof InetSocketAddress addr)
                ip = addr.getAddress().getHostAddress();

            String clientBrand = server.getServerModName();

            String message = eventConfig.getMessage()
                    .replace("{player}", player.getName().getString())
                    .replace("{uuid}", player.getUUID().toString())
                    .replace("{ip}", ip)
                    .replace("{client}", clientBrand)
                    .replace("{time}", LocalDateTime.now().format(TIME_FORMATTER));

            var notifier = ServerHelperMod.getNotifier();
            if (notifier.isEnabled()) {
                notifier.send(message);
            }
        });
    }
}
