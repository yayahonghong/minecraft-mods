package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class OpChangeHandler {
    private static Set<String> cachedOps = new HashSet<>();
    private static int tickCounter = 0;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            cachedOps = server.getPlayerList().getOps().getEntries().stream()
                    .map(e -> e.getUser().name()).collect(Collectors.toSet());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 100 != 0) return;

            ModConfig config = ServerHelperMod.configManager.getConfig();
            var ec = config.getEvents().get("op_change");
            if (ec == null || !ec.isEnabled()) return;

            Set<String> currentOps = server.getPlayerList().getOps().getEntries().stream()
                    .map(e -> e.getUser().name()).collect(Collectors.toSet());

            if (!currentOps.equals(cachedOps)) {
                for (String name : currentOps) {
                    if (!cachedOps.contains(name))
                        sendOpNotif(name, "granted", config, ec.getMessage());
                }
                for (String name : cachedOps) {
                    if (!currentOps.contains(name))
                        sendOpNotif(name, "revoked", config, ec.getMessage());
                }
                cachedOps = new HashSet<>(currentOps);
            }
        });
    }

    private static void sendOpNotif(String player, String status, ModConfig config, String template) {
        String msg = template.replace("{player}", player).replace("{status}", status);
        var n = new QQNotifier(config.getQq(), ServerHelperMod.qqWSClient);
        if (n.isEnabled()) Thread.ofVirtual().start(() -> n.send(null, msg));
    }
}
