package com.ysh.serverhelper;

import com.ysh.serverhelper.config.ModConfigManager;
import com.ysh.serverhelper.command.HelperCommand;
import com.ysh.serverhelper.handler.*;
import com.ysh.serverhelper.qqcmd.QQCommandHandler;
import com.ysh.serverhelper.utils.ServerI18n;
import com.ysh.serverhelper.ws.WSClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHelperMod implements ModInitializer {
    public static final String MOD_ID = "serverhelper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfigManager configManager;
    public static WSClient wsClient;
    public static com.ysh.serverhelper.notifier.Notifier getNotifier() {
        return new com.ysh.serverhelper.notifier.QQNotifier(configManager.getConfig().getQq(), wsClient);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ServerHelper");
        configManager = new ModConfigManager();
        configManager.load();
        
        ServerI18n.init();

        wsClient = new WSClient();

        PlayerJoinHandler.register();
        PlayerQuitHandler.register();
        PlayerDeathHandler.register();
        AdvancementHandler.register();
        ChatHandler.register();
        ServerLifecycleHandler.register();

        wsClient.connect(configManager.getConfig().getQq());
        wsClient.setEventListener(json -> QQCommandHandler.handle(json, configManager.getConfig().getQq()));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            QQCommandHandler.init(server, wsClient);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (wsClient != null) wsClient.disconnect();
        });

        OpChangeHandler.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> HelperCommand.register(dispatcher));
    }
}
