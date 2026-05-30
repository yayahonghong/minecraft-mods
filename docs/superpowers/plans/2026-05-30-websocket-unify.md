# WebSocket 统一改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `serverhelper` 模组的 NapCat 通信全部统一为一条 WebSocket 连接，删除 HTTP API 调用。

**Architecture:** 新建 `QQWSClient` 作为独立 WS 传输层，提供 `sendAction(action, params)` 方法（基于 OneBot v11 echo 机制），`QQNotifier` 和 `QQCommandHandler` 内部从 `HttpClient` 改为调用 `QQWSClient` 的 sendAction。

**Tech Stack:** Java 25, Fabric Loom, java.net.http.WebSocket, Gson

---

### Task 1: 创建 QQWSClient — WS 传输层

**Files:**
- Create: `serverhelper/src/main/java/com/ysh/serverhelper/ws/QQWSClient.java`

**职责:** 维护一条 WebSocket 连接，提供收发 API，事件分发

- [ ] **Step 1: 创建 QQWSClient.java**

```java
package com.ysh.serverhelper.ws;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import net.minecraft.server.MinecraftServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class QQWSClient {
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final AtomicLong echoCounter = new AtomicLong(0);
    private final ConcurrentHashMap<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

    private volatile WebSocket webSocket;
    private volatile boolean active;
    private String wsUrl;
    private ModConfig.QQConfig config;
    private Consumer<String> eventListener;
    private MinecraftServer mcServer;

    public void connect(ModConfig.QQConfig qqConfig, MinecraftServer server) {
        this.config = qqConfig;
        this.mcServer = server;

        String apiUrl = qqConfig.getApiUrl().replaceAll("/+$", "");
        this.wsUrl = (apiUrl.startsWith("https://")
                ? "wss://" + apiUrl.substring(8)
                : "ws://" + apiUrl.substring(7)) + "/ws";

        if (!qqConfig.getToken().isEmpty()) {
            wsUrl += "?access_token=" + qqConfig.getToken();
        }

        active = true;
        connectInternal();
    }

    private void connectInternal() {
        if (!active) return;
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WsListener())
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    ServerHelperMod.LOGGER.info("QQ WebSocket connected to {}", wsUrl);
                })
                .exceptionally(e -> {
                    ServerHelperMod.LOGGER.warn("QQ WS connect failed, retry in 5s: {}", e.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (!active) return;
        Thread.startVirtualThread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) { return; }
            if (active) connectInternal();
        });
    }

    public void disconnect() {
        active = false;
        for (var entry : pendingRequests.entrySet()) {
            entry.getValue().completeExceptionally(new RuntimeException("WS disconnected"));
        }
        pendingRequests.clear();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Server stopping");
            webSocket = null;
        }
    }

    public CompletableFuture<JsonObject> sendAction(String action, JsonObject params) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        if (webSocket == null || !active) {
            future.completeExceptionally(new RuntimeException("WS not connected"));
            return future;
        }

        String echo = "req_" + echoCounter.incrementAndGet();
        pendingRequests.put(echo, future);

        JsonObject payload = new JsonObject();
        payload.addProperty("action", action);
        payload.add("params", params);
        payload.addProperty("echo", echo);

        webSocket.sendText(GSON.toJson(payload), true);

        // 超时清理
        Thread.startVirtualThread(() -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) { }
            CompletableFuture<JsonObject> f = pendingRequests.remove(echo);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(new TimeoutException("WS action timeout: " + action));
            }
        });

        return future;
    }

    public void setEventListener(Consumer<String> listener) {
        this.eventListener = listener;
    }

    private class WsListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            String text = data.toString();
            try {
                JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
                // 检查是否是 echo 响应
                if (obj.has("echo") && !obj.get("echo").isJsonNull()) {
                    String echo = obj.get("echo").getAsString();
                    CompletableFuture<JsonObject> future = pendingRequests.remove(echo);
                    if (future != null) {
                        future.complete(obj);
                    }
                } else if (eventListener != null) {
                    eventListener.accept(text);
                }
            } catch (Exception e) {
                ServerHelperMod.LOGGER.warn("QQ WS parse error", e);
            }
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            ServerHelperMod.LOGGER.warn("QQ WS error: {}", error.getMessage());
            webSocket = null;
            for (var entry : pendingRequests.entrySet()) {
                entry.getValue().completeExceptionally(error);
            }
            pendingRequests.clear();
            scheduleReconnect();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            ServerHelperMod.LOGGER.warn("QQ WS closed ({}/{}), reconnect in 5s", statusCode, reason);
            webSocket = null;
            for (var entry : pendingRequests.entrySet()) {
                entry.getValue().completeExceptionally(new RuntimeException("WS closed"));
            }
            pendingRequests.clear();
            scheduleReconnect();
            return WebSocket.Listener.super.onClose(ws, statusCode, reason);
        }
    }
}
```

- [ ] **Step 2: 确认编译**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

### Task 2: 修改 QQCommandHandler — 改用 WS 发送

**Files:**
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/qqcmd/QQCommandHandler.java`

将 `sendToGroup` 从 `HttpClient` 改为调用 `QQWSClient.sendAction`。`init()` 新增接受 `QQWSClient` 参数。

- [ ] **Step 1: 重写 QQCommandHandler.java**

```java
package com.ysh.serverhelper.qqcmd;

import com.google.gson.JsonObject;
import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.ws.QQWSClient;
import net.minecraft.server.MinecraftServer;

import java.util.stream.Collectors;

public class QQCommandHandler {
    private static MinecraftServer server;
    private static QQWSClient wsClient;

    public static void init(MinecraftServer mcServer, QQWSClient client) {
        server = mcServer;
        wsClient = client;
    }

    public static void handle(String jsonBody, ModConfig.QQConfig config) {
        try {
            JsonObject obj = com.google.gson.JsonParser.parseString(jsonBody).getAsJsonObject();

            if (!obj.has("post_type") || !"message".equals(obj.get("post_type").getAsString())) {
                return;
            }

            if (!obj.has("message_type") || !"group".equals(obj.get("message_type").getAsString())) {
                return;
            }

            if (!obj.has("user_id") || !obj.has("group_id") || (!obj.has("raw_message") && !obj.has("message"))) {
                return;
            }

            long userId = obj.get("user_id").getAsLong();
            long groupId = obj.get("group_id").getAsLong();

            if (groupId != config.getGroupId()) {
                return;
            }

            String rawMsg = obj.has("raw_message") ? obj.get("raw_message").getAsString().trim() : obj.get("message").getAsString().trim();
            String prefix = config.getCommandPrefix();

            if (!rawMsg.startsWith(prefix)) return;
            String cmd = rawMsg.substring(prefix.length()).trim();
            if (cmd.isEmpty()) return;

            boolean isAdmin = config.getAdminQq().contains(userId);
            String response = executeCommand(cmd, isAdmin);

            if (response != null) {
                sendToGroup(groupId, response);
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

    private static void sendToGroup(long groupId, String text) {
        JsonObject params = new JsonObject();
        params.addProperty("group_id", groupId);
        params.addProperty("message", text);
        wsClient.sendAction("send_group_msg", params);
    }
}
```

- [ ] **Step 2: 确认编译**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

### Task 3: 修改 QQNotifier — 改用 WS 发送

**Files:**
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/notifier/QQNotifier.java`

将 `HttpClient` 替换为 `QQWSClient.sendAction`，构造器新增 `QQWSClient` 参数。

- [ ] **Step 1: 重写 QQNotifier.java**

```java
package com.ysh.serverhelper.notifier;

import com.google.gson.JsonObject;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.ws.QQWSClient;

public class QQNotifier implements Notifier {
    private final QQWSClient wsClient;
    private final ModConfig.QQConfig config;

    public QQNotifier(ModConfig.QQConfig config, QQWSClient wsClient) {
        this.config = config;
        this.wsClient = wsClient;
    }

    @Override
    public String getName() { return "QQ(NapCat)"; }

    @Override
    public boolean isEnabled() {
        return config.isEnabled() && !config.getApiUrl().isEmpty() && config.getGroupId() > 0;
    }

    @Override
    public void send(JoinNotification notification, String message) {
        if (!isEnabled()) return;
        JsonObject params = new JsonObject();
        params.addProperty("group_id", config.getGroupId());
        params.addProperty("message", message);
        wsClient.sendAction("send_group_msg", params);
    }
}
```

- [ ] **Step 2: 确认编译**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

### Task 4: 修改 ServerHelperMod — 创建 WS 客户端并注入

**Files:**
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/ServerHelperMod.java`

创建 `QQWSClient` 单例，替换 `QQCommandWSClient`，注入到 `QQCommandHandler` 和 `QQNotifier`。

- [ ] **Step 1: 重写 ServerHelperMod.java**

```java
package com.ysh.serverhelper;

import com.ysh.serverhelper.config.ModConfigManager;
import com.ysh.serverhelper.command.HelperCommand;
import com.ysh.serverhelper.handler.*;
import com.ysh.serverhelper.qqcmd.QQCommandHandler;
import com.ysh.serverhelper.utils.ServerI18n;
import com.ysh.serverhelper.ws.QQWSClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHelperMod implements ModInitializer {
    public static final String MOD_ID = "serverhelper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfigManager configManager;
    public static QQWSClient qqWSClient;

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
            qqWSClient = new QQWSClient();
            qqWSClient.connect(configManager.getConfig().getQq(), server);
            qqWSClient.setEventListener(json -> QQCommandHandler.handle(json, configManager.getConfig().getQq()));

            QQCommandHandler.init(server, qqWSClient);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (qqWSClient != null) qqWSClient.disconnect();
        });

        OpChangeHandler.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HelperCommand.register(dispatcher);
        });
    }
}
```

- [ ] **Step 2: 确认编译**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

### Task 5: 删除 QQCommandWSClient

**Files:**
- Delete: `serverhelper/src/main/java/com/ysh/serverhelper/qqcmd/QQCommandWSClient.java`

- [ ] **Step 1: 删除文件**

Run: `Remove-Item -LiteralPath "E:\minecraft-mods\serverhelper\src\main\java\com\ysh\serverhelper\qqcmd\QQCommandWSClient.java"`

- [ ] **Step 2: 确认编译通过**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

### Task 6: 更新所有 QQNotifier 创建点 — 传入 WS 客户端

**Files:**
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/PlayerJoinHandler.java:40`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/PlayerQuitHandler.java:34`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/PlayerDeathHandler.java:29`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/AdvancementHandler.java:45`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/ChatHandler.java:27`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/ServerLifecycleHandler.java:14,23`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/handler/OpChangeHandler.java:49`
- Modify: `serverhelper/src/main/java/com/ysh/serverhelper/command/HelperCommand.java:49`

- [ ] **Step 1: 修改所有 handler 中 `new QQNotifier(config.getQq())` 为 `new QQNotifier(config.getQq(), ServerHelperMod.qqWSClient)`**

共 8 个文件 9 处。逐文件修改。

- [ ] **Step 2: 确认编译通过**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

### Task 7: 更新文档

**Files:**
- Modify: `serverhelper/README.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 更新 serverhelper/README.md**

将架构图中 HTTP → WS 的描述更新，确保反映现在的全 WS 架构。

- [ ] **Step 2: 更新 AGENTS.md**

更新模组架构描述，删除 HTTP client 相关描述。

### Task 8: 最终构建确认

- [ ] **Step 1: 全量构建**

Run: `./gradlew --no-daemon --console=plain :serverhelper:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行测试**

Run: `./gradlew --no-daemon --console=plain :serverhelper:test`
Expected: Tests PASS

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(serverhelper): 统一为 WebSocket 双向通信，删除 HTTP API 调用"
```
