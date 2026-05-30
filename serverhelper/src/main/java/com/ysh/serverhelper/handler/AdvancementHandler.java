package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import com.ysh.serverhelper.utils.ServerI18n;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdvancementHandler {
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register() {
        // Now handled by PlayerAdvancementsMixin (event-driven push model)
    }

    public static void onAdvancementGranted(ServerPlayer player, AdvancementHolder holder) {
        if (holder.value().display().isEmpty() || !holder.value().display().get().shouldAnnounceChat()) {
            return; // Skip hidden/system advancements and recipes
        }

        ModConfig config = ServerHelperMod.configManager.getConfig();
        var ec = config.getEvents().get("advancement");
        if (ec == null || !ec.isEnabled()) return;
        if (config.getExcludedPlayers().contains(player.getName().getString())) return;

        String name;
        Component titleComponent = holder.value().display().get().getTitle();
        if (titleComponent.getContents() instanceof TranslatableContents tc) {
            String translated = ServerI18n.get(tc.getKey());
            name = translated != null ? translated : titleComponent.getString();
        } else {
            name = titleComponent.getString();
        }
        String msg = ec.getMessage()
                .replace("{player}", player.getName().getString())
                .replace("{advancement}", name)
                .replace("{time}", LocalDateTime.now().format(TF));

        var n = new QQNotifier(config.getQq(), ServerHelperMod.qqWSClient);
        if (n.isEnabled()) {
            Thread.ofVirtual().start(() -> n.send(null, msg));
        }
    }
}
