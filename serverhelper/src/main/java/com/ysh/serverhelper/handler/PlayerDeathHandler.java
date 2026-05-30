package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerDeathHandler {
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var ec = config.getEvents().get("death");
            if (ec == null || !ec.isEnabled()) return;
            if (config.getExcludedPlayers().contains(player.getName().getString())) return;

            String msg = ec.getMessage()
                    .replace("{player}", player.getName().getString())
                    .replace("{death_message}", source.getLocalizedDeathMessage(player).getString())
                    .replace("{time}", LocalDateTime.now().format(TF));

            var n = new QQNotifier(config.getQq(), ServerHelperMod.qqWSClient);
            if (n.isEnabled()) Thread.ofVirtual().start(() -> n.send(null, msg));
        });
    }
}
