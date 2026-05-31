package com.ysh.serverhelper.handler;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import java.util.concurrent.TimeUnit;

public class ServerLifecycleHandler {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var eventConfig = config.getEvents().get("server_start");
            if (eventConfig != null && eventConfig.isEnabled()) {
                var notifier = ServerHelperMod.getNotifier();
                if (notifier.isEnabled()) notifier.send(eventConfig.getMessage());
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ModConfig config = ServerHelperMod.configManager.getConfig();
            var eventConfig = config.getEvents().get("server_stop");
            if (eventConfig != null && eventConfig.isEnabled()) {
                var notifier = ServerHelperMod.getNotifier();
                if (notifier.isEnabled()) {
                    try {
                        notifier.send(eventConfig.getMessage()).get(3, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        ServerHelperMod.LOGGER.warn("Failed or interrupted while sending server_stop message");
                    }
                }
            }
        });
    }
}
