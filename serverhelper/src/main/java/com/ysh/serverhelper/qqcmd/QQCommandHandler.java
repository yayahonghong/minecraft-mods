package com.ysh.serverhelper.qqcmd;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * QQ 命令处理（AstrBot 版）。
 * 由 AstrBotPoller 在 Minecraft 主线程调用，输入原始消息文本，返回应答文本。
 */
public class QQCommandHandler {
    private static MinecraftServer server;

    private static final List<MenuSession.MenuItem> MENU_ITEMS = List.of(
            new MenuSession.MenuItem("👥 在线玩家", "list"),
            new MenuSession.MenuItem("📊 服务器状态", "status"),
            new MenuSession.MenuItem("🎮 服务器延迟", "tps"),
            new MenuSession.MenuItem("🔄 刷新菜单", "menu")
    );

    public static void init(MinecraftServer mcServer) {
        server = mcServer;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    /**
     * 处理一条来自 QQ 群的消息。
     *
     * @param rawMsg 消息原文
     * @param userId 发送者 QQ 号
     * @param config AstrBot 配置
     * @return 需要回复到群里的文本；返回 null 表示无需回复
     */
    public static String handle(String rawMsg, String userId, ModConfig.AstrBotConfig config) {
        try {
            rawMsg = rawMsg.trim();
            String prefix = config.getCommandPrefix();

            if (rawMsg.startsWith(prefix)) {
                String cmd = rawMsg.substring(prefix.length()).trim();
                if (cmd.isEmpty()) return null;

                String builtin = handleBuiltinCommand(cmd, userId);
                if (builtin != null) return builtin;

                boolean isAdmin = config.getAdminQq().contains(userId);
                return executeCommand(cmd, isAdmin);
            }

            String rawLower = rawMsg.toLowerCase();
            if (rawLower.equals("菜单") || rawLower.equals("帮助")) {
                MenuSessionManager.set(new MenuSession(userId, "", MENU_ITEMS));
                return buildMenuText();
            }
            if (rawLower.equals("取消")) {
                if (MenuSessionManager.hasActive(userId)) {
                    MenuSessionManager.remove(userId);
                    return "已取消菜单";
                }
                return null;
            }

            MenuSession session = MenuSessionManager.get(userId);
            if (session != null) {
                return handleMenuChoice(userId, rawMsg, config);
            }
            return null;
        } catch (Exception e) {
            ServerHelperMod.LOGGER.warn("QQ command handler error", e);
            return "命令处理出错";
        }
    }

    private static String handleBuiltinCommand(String cmd, String userId) {
        if (cmd.equalsIgnoreCase("cancel")) {
            if (MenuSessionManager.hasActive(userId)) {
                MenuSessionManager.remove(userId);
                return "已取消菜单";
            }
            return null;
        }
        String action = cmd.split(" ", 2)[0].toLowerCase();
        if (action.equals("menu") || action.equals("help")) {
            MenuSessionManager.set(new MenuSession(userId, "", MENU_ITEMS));
            return buildMenuText();
        }
        return null;
    }

    private static String handleMenuChoice(String userId, String rawMsg, ModConfig.AstrBotConfig config) {
        try {
            int choice = Integer.parseInt(rawMsg.trim());
            var items = MENU_ITEMS;
            if (choice >= 1 && choice <= items.size()) {
                MenuSession.MenuItem item = items.get(choice - 1);
                MenuSessionManager.remove(userId);

                if (item.action().equals("menu")) {
                    MenuSessionManager.set(new MenuSession(userId, "", MENU_ITEMS));
                    return buildMenuText();
                }

                boolean isAdmin = config.getAdminQq().contains(userId);
                return executeCommand(item.action(), isAdmin);
            } else {
                return "无效选项（请输入 1-" + items.size() + "），回复「取消」退出";
            }
        } catch (NumberFormatException e) {
            return "请输入有效编号，回复「取消」退出菜单";
        }
    }

    private static String executeCommand(String cmd, boolean isAdmin) {
        String[] parts = cmd.split(" ", 2);
        String action = parts[0].toLowerCase();

        return switch (action) {
            case "list" -> {
                if (server == null) yield "服务器未就绪";
                var players = server.getPlayerList().getPlayers();
                if (players.isEmpty()) yield "当前没有在线玩家";
                var names = players.stream().map(p -> p.getName().getString()).collect(Collectors.toList());
                yield "在线玩家 (" + names.size() + "): " + String.join(", ", names);
            }
            case "tps" -> {
                if (server == null) yield "服务器未就绪";
                long[] tickTimes = server.getTickTimesNanos();
                long total = 0;
                for (long t : tickTimes) total += t;
                double avgNanos = (double) total / tickTimes.length;
                double tps = Math.min(1_000_000_000.0 / avgNanos, 20.0);
                yield String.format("TPS: %.1f", tps);
            }
            case "status" -> {
                if (server == null) yield "服务器未就绪";
                var players = server.getPlayerList().getPlayers();
                long[] tickTimes = server.getTickTimesNanos();
                long total = 0;
                for (long t : tickTimes) total += t;
                double avgNanos = (double) total / tickTimes.length;
                double tps = Math.min(1_000_000_000.0 / avgNanos, 20.0);
                long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
                long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                yield String.format("在线: %d | TPS: %.1f | 内存: %d/%dMB | Uptime: %dh",
                        players.size(), tps, usedMem, maxMem, server.getTickCount() / 72000);
            }
            case "say" -> {
                if (!isAdmin) yield "无权执行此指令";
                if (parts.length < 2) yield "用法: #say <消息>";
                if (server == null) yield "服务器未就绪";
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "say " + parts[1]);
                yield "已广播: " + parts[1];
            }
            case "cmd" -> {
                if (!isAdmin) yield "无权执行此指令";
                if (parts.length < 2) yield "用法: #cmd <命令>";
                if (server == null) yield "服务器未就绪";
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), parts[1]);
                yield "指令已执行: " + parts[1];
            }
            default -> "未知指令，发送 #menu 或「菜单」查看可用指令";
        };
    }

    private static String buildMenuText() {
        StringBuilder sb = new StringBuilder("🏠 MC 服务器菜单\n回复编号执行命令，回复「取消」退出\n\n");
        for (int i = 0; i < MENU_ITEMS.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(MENU_ITEMS.get(i).label()).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
