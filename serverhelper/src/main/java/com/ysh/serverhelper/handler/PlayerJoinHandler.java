package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerJoinHandler {
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var ec = config.getEvents().get("join");
            if (ec == null || !ec.isEnabled()) return;
            if (config.getExcludedPlayers().contains(player.getName().getString())) return;

            String ip = "";
            if (handler.getRemoteAddress() instanceof InetSocketAddress addr)
                ip = addr.getAddress().getHostAddress();

            String clientBrand = server.getServerModName();

            String message = ec.getMessage()
                    .replace("{player}", player.getName().getString())
                    .replace("{uuid}", player.getUUID().toString())
                    .replace("{ip}", ip)
                    .replace("{client}", clientBrand)
                    .replace("{time}", LocalDateTime.now().format(TF));

            var n = new QQNotifier(config.getQq(), ServerHelperMod.wsClient);
            if (n.isEnabled())
                Thread.ofVirtual().start(() -> {
                    try { n.send(message); }
                    catch (Exception e) { ServerHelperMod.LOGGER.warn("{} failed", n.getName(), e); }
                });
        });
    }
}
