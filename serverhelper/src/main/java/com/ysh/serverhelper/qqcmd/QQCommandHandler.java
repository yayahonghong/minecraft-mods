package com.ysh.serverhelper.qqcmd;

import com.google.gson.JsonObject;
import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.ws.WSClient;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.stream.Collectors;

public class QQCommandHandler {
    private static MinecraftServer server;
    private static WSClient wsClient;

    private static final List<MenuSession.MenuItem> MENU_ITEMS = List.of(
            new MenuSession.MenuItem("👥 在线玩家", "list"),
            new MenuSession.MenuItem("📊 服务器状态", "status"),
            new MenuSession.MenuItem("🎮 服务器延迟", "tps"),
            new MenuSession.MenuItem("🔄 刷新菜单", "menu")
    );

    public static void init(MinecraftServer mcServer, WSClient client) {
        server = mcServer;
        wsClient = client;
    }

    public static void handle(String jsonBody, ModConfig.QQConfig config) {
        try {
            JsonObject obj = com.google.gson.JsonParser.parseString(jsonBody).getAsJsonObject();

            if (!obj.has("post_type") || !"message".equals(obj.get("post_type").getAsString())) return;
            if (!obj.has("message_type") || !"group".equals(obj.get("message_type").getAsString())) return;
            if (!obj.has("user_id") || !obj.has("group_id") || (!obj.has("raw_message") && !obj.has("message"))) return;

            long userId = obj.get("user_id").getAsLong();
            long groupId = obj.get("group_id").getAsLong();
            if (groupId != config.getGroupId()) return;

            String rawMsg = obj.has("raw_message") ? obj.get("raw_message").getAsString().trim() : obj.get("message").getAsString().trim();
            String prefix = config.getCommandPrefix();

            if (rawMsg.startsWith(prefix)) {
                String cmd = rawMsg.substring(prefix.length()).trim();
                if (cmd.isEmpty()) return;

                if (handleBuiltinCommand(cmd, userId, groupId, config)) return;
                boolean isAdmin = config.getAdminQq().contains(userId);
                String response = executeCommand(cmd, isAdmin);
                sendToGroup(groupId, response);
                return;
            }

            String rawLower = rawMsg.toLowerCase();
            if (rawLower.equals("菜单") || rawLower.equals("帮助")) {
                MenuSession.set(new MenuSession(userId, groupId, MENU_ITEMS));
                sendToGroup(groupId, buildMenuText());
                return;
            }
            if (rawLower.equals("取消")) {
                if (MenuSession.hasActive(userId)) {
                    MenuSession.remove(userId);
                    sendToGroup(groupId, "已取消菜单");
                }
                return;
            }

            MenuSession session = MenuSession.get(userId);
            if (session != null) {
                handleMenuChoice(userId, groupId, rawMsg, config);
            }
        } catch (Exception e) {
            ServerHelperMod.LOGGER.warn("QQ command handler error", e);
        }
    }

    private static boolean handleBuiltinCommand(String cmd, long userId, long groupId, ModConfig.QQConfig config) {
        if (cmd.equalsIgnoreCase("cancel")) {
            if (MenuSession.hasActive(userId)) {
                MenuSession.remove(userId);
                sendToGroup(groupId, "已取消菜单");
            }
            return true;
        }
        String action = cmd.split(" ", 2)[0].toLowerCase();
        if (action.equals("menu") || action.equals("help")) {
            MenuSession.set(new MenuSession(userId, groupId, MENU_ITEMS));
            sendToGroup(groupId, buildMenuText());
            return true;
        }
        return false;
    }

    private static void handleMenuChoice(long userId, long groupId, String rawMsg, ModConfig.QQConfig config) {
        try {
            int choice = Integer.parseInt(rawMsg.trim());
            var items = MENU_ITEMS;
            if (choice >= 1 && choice <= items.size()) {
                MenuSession.MenuItem item = items.get(choice - 1);
                MenuSession.remove(userId);

                if (item.action().equals("menu")) {
                    MenuSession.set(new MenuSession(userId, groupId, MENU_ITEMS));
                    sendToGroup(groupId, buildMenuText());
                    return;
                }

                boolean isAdmin = config.getAdminQq().contains(userId);
                String response = executeCommand(item.action(), isAdmin);
                sendToGroup(groupId, response);
            } else {
                sendToGroup(groupId, "无效选项（请输入 1-" + items.size() + "），回复「取消」退出");
            }
        } catch (NumberFormatException e) {
            sendToGroup(groupId, "请输入有效编号，回复「取消」退出菜单");
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

    private static void sendToGroup(long groupId, String text) {
        JsonObject params = new JsonObject();
        params.addProperty("group_id", groupId);
        params.addProperty("message", text);
        wsClient.sendAction("send_group_msg", params);
    }
}
