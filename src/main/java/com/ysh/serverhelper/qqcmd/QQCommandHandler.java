package com.ysh.serverhelper.qqcmd;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.notifier.QQNotifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class QQCommandHandler {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static MinecraftServer server;

    public static void init(MinecraftServer mcServer) {
        server = mcServer;
    }

    public static void handle(String jsonBody, ModConfig.QQConfig config) {
        try {
            JsonObject obj = JsonParser.parseString(jsonBody).getAsJsonObject();
            
            // Ignore non-messages (like heartbeats)
            if (!obj.has("post_type") || !"message".equals(obj.get("post_type").getAsString())) {
                return;
            }
            
            // Only handle group messages for now
            if (!obj.has("message_type") || !"group".equals(obj.get("message_type").getAsString())) {
                return;
            }
            
            if (!obj.has("user_id") || !obj.has("group_id") || (!obj.has("raw_message") && !obj.has("message"))) {
                return;
            }

            long userId = obj.get("user_id").getAsLong();
            long groupId = obj.get("group_id").getAsLong();
            
            // Configured target group check
            if (groupId != config.getGroupId()) {
                return; // Ignore messages from other groups
            }

            String rawMsg = obj.has("raw_message") ? obj.get("raw_message").getAsString().trim() : obj.get("message").getAsString().trim();
            String prefix = config.getCommandPrefix();

            if (!rawMsg.startsWith(prefix)) return;
            String cmd = rawMsg.substring(prefix.length()).trim();
            if (cmd.isEmpty()) return;

            boolean isAdmin = config.getAdminQq().contains(userId);
            String response = executeCommand(cmd, isAdmin);

            if (response != null) {
                sendToGroup(config, groupId, response);
            }
        } catch (Exception e) {
            ServerHelperMod.LOGGER.warn("QQ command handler error", e);
        }
    }

    private static String executeCommand(String cmd, boolean isAdmin) {
        String[] parts = cmd.split(" ", 2);
        String action = parts[0].toLowerCase();

        return switch (action) {
            case "help" -> {
                StringBuilder sb = new StringBuilder("=== ServerHelper 可用指令 ===\n");
                sb.append("#help - 查看此帮助菜单\n");
                sb.append("#list - 查看当前在线玩家列表\n");
                sb.append("#tps - 查看服务器当前 TPS\n");
                sb.append("#status - 查看服务器综合状态(内存/在线等)");
                if (isAdmin) {
                    sb.append("\n\n[管理员专用]\n");
                    sb.append("#say <内容> - 以服务器名义广播消息\n");
                    sb.append("#cmd <命令> - 执行后台控制台命令");
                }
                yield sb.toString();
            }
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
                double tps = Math.min(1_000_000_000.0 / ((double) total / tickTimes.length), 20.0);
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
            default -> "未知指令，发送 #help 查看可用指令";
        };
    }

    private static void sendToGroup(ModConfig.QQConfig config, long groupId, String text) {
        String json = "{\"group_id\":" + groupId + ",\"message\":\"" + escapeJson(text) + "\"}";
        String url = config.getApiUrl().replaceAll("/+$", "") + "/send_group_msg";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8");
        if (!config.getToken().isEmpty())
            builder.header("Authorization", "Bearer " + config.getToken());

        client.sendAsync(builder
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String escapeJson(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
