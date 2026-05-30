package com.ysh.serverhelper.command;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.util.concurrent.CompletableFuture;

public class HelperCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("helper")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ModConfig config = ServerHelperMod.configManager.getConfig();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6=== ServerHelper Status ==="), false);

                            String qqIcon = config.getQq().isEnabled() ? "§a✔" : "§c✘";
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§eQQ: " + qqIcon), false);

                            config.getEvents().forEach((key, ec) -> {
                                String icon = ec.isEnabled() ? "§a✔" : "§c✘";
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§e" + key + ": " + icon), false);
                            });

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§e排除: " + config.getExcludedPlayers()), false);
                            return 1;
                        }))
                .then(Commands.literal("test")
                        .executes(ctx -> {
                            ModConfig config = ServerHelperMod.configManager.getConfig();
                            String msg = config.getEvents().get("join").getMessage()
                                    .replace("{player}", "TestPlayer")
                                    .replace("{uuid}", "00000000-0000-0000-0000-000000000000")
                                    .replace("{ip}", "127.0.0.1")
                                    .replace("{client}", "Fabric")
                                    .replace("{time}", "test");

                            var n = new QQNotifier(config.getQq(), ServerHelperMod.qqWSClient);
                            if (n.isEnabled()) {
                                n.send(null, msg);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§aTest notification sent!"), false);
                            } else {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§cQQ notifier is not enabled"), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            ServerHelperMod.configManager.reload();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§aConfiguration reloaded!"), false);
                            return 1;
                        }))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    ModConfig config = ServerHelperMod.configManager.getConfig();
                                    config.getEvents().keySet().forEach(builder::suggest);
                                    builder.suggest("qq");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String event = StringArgumentType.getString(ctx, "event");
                                    ModConfig config = ServerHelperMod.configManager.getConfig();

                                    if ("qq".equals(event)) {
                                        config.getQq().setEnabled(!config.getQq().isEnabled());
                                        String status = config.getQq().isEnabled() ? "§a已启用" : "§c已禁用";
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§eQQ 通知: " + status), false);
                                        ServerHelperMod.configManager.save();
                                        return 1;
                                    }

                                    var ec = config.getEvents().get(event);
                                    if (ec == null) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§c未知事件: " + event), false);
                                        return 0;
                                    }
                                    ec.setEnabled(!ec.isEnabled());
                                    String status = ec.isEnabled() ? "§a已启用" : "§c已禁用";
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§e事件 " + event + ": " + status), false);
                                    ServerHelperMod.configManager.save();
                                    return 1;
                                })))
        );
    }
}
