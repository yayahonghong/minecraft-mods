package com.ysh.serverbot;

import com.ysh.serverbot.bot.BotManager;
import com.ysh.serverbot.command.BotCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerBotMod implements ModInitializer {
    public static final String MOD_ID = "serverbot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ServerBot");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            BotCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
            BotManager.getInstance().shutdown(server));
    }
}
