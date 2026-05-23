package com.ysh.smartplace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartPlaceMod implements ModInitializer {
    public static final String MOD_ID = "smartplace";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("SmartPlace loaded");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("smartplace")
                .then(Commands.literal("toggle")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        boolean enabled = SmartPlaceState.toggle(player);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§e智能放置已" + (enabled ? "§a启用" : "§c禁用")), false);
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        boolean enabled = SmartPlaceState.isEnabled(player);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§e智能放置: " + (enabled ? "§a已启用" : "§c已禁用")), false);
                        return 1;
                    })));
        });
    }
}
