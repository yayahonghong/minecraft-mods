package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.utils.ServerI18n;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdvancementHandler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        // Now handled by PlayerAdvancementsMixin (event-driven push model)
    }

    public static void onAdvancementGranted(ServerPlayer player, AdvancementHolder holder) {
        if (holder.value().display().isEmpty() || !holder.value().display().get().shouldAnnounceChat()) {
            return; // Skip hidden/system advancements and recipes
        }

        ModConfig config = ServerHelperMod.configManager.getConfig();
        var eventConfig = config.getEvents().get("advancement");
        if (eventConfig == null || !eventConfig.isEnabled()) return;
        if (config.getExcludedPlayers().contains(player.getName().getString())) return;

        String name;
        Component titleComponent = holder.value().display().get().getTitle();
        if (titleComponent.getContents() instanceof TranslatableContents translatableContents) {
            String translated = ServerI18n.get(translatableContents.getKey());
            name = translated != null ? translated : titleComponent.getString();
        } else {
            name = titleComponent.getString();
        }
        String msg = eventConfig.getMessage()
                .replace("{player}", player.getName().getString())
                .replace("{advancement}", name)
                .replace("{time}", LocalDateTime.now().format(TIME_FORMATTER));

        var notifier = ServerHelperMod.getNotifier();
        if (notifier.isEnabled()) {
            notifier.send(msg);
        }
    }
}
