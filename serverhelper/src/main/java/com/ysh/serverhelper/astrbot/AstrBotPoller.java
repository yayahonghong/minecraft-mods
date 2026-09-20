package com.ysh.serverhelper.astrbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.qqcmd.QQCommandHandler;
import net.minecraft.server.MinecraftServer;

/**
 * 命令轮询线程：定期从 mc_bridge 插件拉取 QQ 群命令，
 * 交回 Minecraft 主线程执行后，将结果通过插件回传到群。
 */
public class AstrBotPoller {
    private volatile boolean active;
    private Thread thread;
    private volatile long lastConfigLogMs;
    private volatile long lastPollFailLogMs;

    public synchronized void start() {
        if (active) return;
        active = true;
        thread = Thread.ofVirtual().name("serverhelper-astrbot-poller").unstarted(this::runLoop);
        thread.start();
        ServerHelperMod.LOGGER.info("AstrBot 命令轮询已启动");
    }

    private static String maskToken(String token) {
        if (token.isEmpty()) return "<空>";
        return token.length() <= 4 ? "****" : token.substring(0, 4) + "****";
    }

    private synchronized void logConfigState(ModConfig.AstrBotConfig cfg) {
        long now = System.currentTimeMillis();
        if (now - lastConfigLogMs < 30_000) return;
        lastConfigLogMs = now;
        ServerHelperMod.LOGGER.info(
                "AstrBot 轮询等待配置: enabled={} bridge_url={} internal_token={} 在线={}",
                cfg.isEnabled(),
                cfg.getBridgeUrl().isEmpty() ? "<未填>" : cfg.getBridgeUrl(),
                maskToken(cfg.getInternalToken()),
                QQCommandHandler.getServer() != null);
    }

    public synchronized void stop() {
        active = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private void runLoop() {
        while (active) {
            try {
                ModConfig.AstrBotConfig cfg = ServerHelperMod.configManager.getConfig().getAstrbot();
                if (!cfg.isEnabled() || cfg.getBridgeUrl().isEmpty() || cfg.getInternalToken().isEmpty()) {
                    logConfigState(cfg);
                    sleepQuietly(3000);
                    continue;
                }

                JsonArray commands;
                try {
                    commands = ServerHelperMod.astrBotClient.pollCommands(5);
                } catch (Exception e) {
                    if (!active) break;
                    long now = System.currentTimeMillis();
                    if (now - lastPollFailLogMs > 15_000) {
                        lastPollFailLogMs = now;
                        ServerHelperMod.LOGGER.warn("AstrBot 轮询失败({}): {}", cfg.getBridgeUrl(), e.toString());
                    }
                    sleepQuietly(Math.max(1000, cfg.getPollIntervalMs()));
                    continue;
                }

                if (!commands.isEmpty()) {
                    ServerHelperMod.LOGGER.info("AstrBot 拉取到 {} 条命令", commands.size());
                }

                for (var element : commands) {
                    JsonObject cmd = element.getAsJsonObject();
                    dispatch(cmd, cfg);
                }
            } catch (Exception e) {
                if (!active) break;
                ServerHelperMod.LOGGER.warn("AstrBot 轮询循环异常", e);
                sleepQuietly(3000);
            }
        }
    }

    private void dispatch(JsonObject cmd, ModConfig.AstrBotConfig cfg) {
        String id = cmd.has("id") ? cmd.get("id").getAsString() : "";
        String userId = cmd.has("user_id") && !cmd.get("user_id").isJsonNull()
                ? cmd.get("user_id").getAsString() : "";
        String text = cmd.has("text") && !cmd.get("text").isJsonNull() ? cmd.get("text").getAsString() : "";
        if (id.isEmpty() || text.isEmpty()) return;

        MinecraftServer server = QQCommandHandler.getServer();
        if (server == null) {
            ServerHelperMod.astrBotClient.replyCommand(id, "服务器未就绪");
            return;
        }

        server.execute(() -> {
            String response;
            try {
                response = QQCommandHandler.handle(text, userId, cfg);
            } catch (Exception e) {
                ServerHelperMod.LOGGER.warn("处理 QQ 命令失败: {}", text, e);
                response = "命令执行出错: " + e.getMessage();
            }
            if (response != null && !response.isEmpty()) {
                // 回传是阻塞 HTTP 请求，放到虚拟线程执行，避免卡住主线程
                final String reply = response;
                Thread.startVirtualThread(() -> ServerHelperMod.astrBotClient.replyCommand(id, reply));
            }
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
