package com.ysh.serverbot.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ysh.serverbot.bot.BotManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class BotCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bot")
            .then(Commands.literal("spawn")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.literal("§c该命令只能由玩家执行"));
                        return 0;
                    }

                    BotManager manager = BotManager.getInstance();
                    if (manager.spawnBot(player) != null) {
                        source.sendSuccess(() -> Component.literal("§a假人生成成功"), true);
                        return 1;
                    }
                    return 0;
                })
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        if (BotManager.getInstance().removeBot(name)) {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a已移除假人 " + name), true);
                            return 1;
                        }
                        ctx.getSource().sendFailure(Component.literal("§c未找到假人 " + name));
                        return 0;
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    BotManager manager = BotManager.getInstance();
                    List<String> bots = manager.listBots();
                    if (bots.isEmpty()) {
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§e当前没有在线假人"), false);
                    } else {
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§6=== 在线假人 (" + bots.size() + "/" + manager.getMaxBots() + ") ==="),
                            false);
                        for (String name : bots) {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal(" §7- §a" + name), false);
                        }
                    }
                    return 1;
                })
            )
        );
    }
}
