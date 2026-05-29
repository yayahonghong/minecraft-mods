package com.ysh.serverhelper;

import com.ysh.serverhelper.config.ModConfigManager;
import com.ysh.serverhelper.command.HelperCommand;
import com.ysh.serverhelper.handler.*;
import com.ysh.serverhelper.qqcmd.QQCommandWSClient;
import com.ysh.serverhelper.utils.ServerI18n;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHelperMod implements ModInitializer {
    public static final String MOD_ID = "serverhelper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfigManager configManager;
    public static QQCommandWSClient qqCommandWSClient;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ServerHelper");
        configManager = new ModConfigManager();
        configManager.load();
        
        ServerI18n.init();

        PlayerJoinHandler.register();
        PlayerQuitHandler.register();
        PlayerDeathHandler.register();
        AdvancementHandler.register();
        ChatHandler.register();
        ServerLifecycleHandler.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            qqCommandWSClient = new QQCommandWSClient();
            qqCommandWSClient.connect(configManager.getConfig().getQq(), server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (qqCommandWSClient != null) qqCommandWSClient.disconnect();
        });

        OpChangeHandler.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HelperCommand.register(dispatcher);
        });
    }
}
