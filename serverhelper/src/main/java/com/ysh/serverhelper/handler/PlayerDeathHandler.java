package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlayerDeathHandler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var eventConfig = config.getEvents().get("death");
            if (eventConfig == null || !eventConfig.isEnabled()) return;
            if (config.getExcludedPlayers().contains(player.getName().getString())) return;

            String msg = eventConfig.getMessage()
                    .replace("{player}", player.getName().getString())
                    .replace("{death_message}", source.getLocalizedDeathMessage(player).getString())
                    .replace("{time}", LocalDateTime.now().format(TIME_FORMATTER));

            var notifier = ServerHelperMod.getNotifier();
            if (notifier.isEnabled()) notifier.send(msg);
        });
    }
}
