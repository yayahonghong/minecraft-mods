package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.JoinNotification;
import com.ysh.serverhelper.notifier.Notifier;
import com.ysh.serverhelper.notifier.QQNotifier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

            String clientBrand = server != null ? server.getServerModName() : "unknown";

            String message = ec.getMessage()
                    .replace("{player}", player.getName().getString())
                    .replace("{uuid}", player.getUUID().toString())
                    .replace("{ip}", ip)
                    .replace("{client}", clientBrand)
                    .replace("{time}", LocalDateTime.now().format(TF));

            var n = new QQNotifier(config.getQq(), ServerHelperMod.qqWSClient);
            if (n.isEnabled())
                Thread.ofVirtual().start(() -> {
                    try { n.send(null, message); }
                    catch (Exception e) { ServerHelperMod.LOGGER.warn("{} failed", n.getName(), e); }
                });
        });
    }
}
