package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerLifecycleHandler {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var ec = config.getEvents().get("server_start");
            if (ec != null && ec.isEnabled()) {
                var n = new QQNotifier(config.getQq());
                if (n.isEnabled()) Thread.ofVirtual().start(() -> n.send(null, ec.getMessage()));
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var ec = config.getEvents().get("server_stop");
            if (ec != null && ec.isEnabled()) {
                var n = new QQNotifier(config.getQq());
                if (n.isEnabled()) {
                    // Start the thread
                    Thread t = Thread.ofVirtual().start(() -> n.send(null, ec.getMessage()));
                    try {
                        // Wait up to 3 seconds for the HTTP request to finish
                        // If we don't wait, the JVM might exit before the network packet is actually sent
                        t.join(3000); 
                    } catch (InterruptedException e) {
                        ServerHelperMod.LOGGER.warn("Interrupted while sending server_stop message");
                    }
                }
            }
        });
    }
}
