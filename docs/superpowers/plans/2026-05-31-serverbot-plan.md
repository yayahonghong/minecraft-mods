# ServerBot 假人模组实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fabric 服务端假人模组，挂机不掉线。

**Architecture:** 自定义 BotNetworkHandler（extends ServerGamePacketListenerImpl）处理静默心跳 + 无操作发包；ServerBotPlayer（extends ServerPlayer）通过 PlayerList 注册；BotManager 管理生命周期。

**Tech Stack:** Minecraft 26.1, Fabric Loader 0.19.2, Fabric API 0.149.0, Java 25, Brigadier 命令 API

---

### Task 1: 创建子模组基础结构

**Files:**
- Create: `serverbot/build.gradle`
- Create: `serverbot/src/main/resources/fabric.mod.json`
- Modify: `settings.gradle`

- [ ] **Step 1: 创建 serverbot/build.gradle**

复制 `serverhelper/build.gradle` 内容，其他不变。

```groovy
plugins {
    id 'net.fabricmc.fabric-loom'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

repositories {
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}

processResources {
    def version = project.version
    inputs.property "version", version
    filesMatching("fabric.mod.json") {
        expand "version": version
    }
}

jar {
    def projectName = project.name
    inputs.property "projectName", projectName
    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}
```

- [ ] **Step 2: 创建 fabric.mod.json**

```json
{
    "schemaVersion": 1,
    "id": "serverbot",
    "version": "${version}",
    "name": "ServerBot",
    "description": "Server bot mod for AFK staying online.",
    "authors": ["YSH"],
    "contact": {},
    "license": "MIT",
    "environment": "server",
    "entrypoints": {
        "main": [
            "com.ysh.serverbot.ServerBotMod"
        ]
    },
    "depends": {
        "fabricloader": ">=0.18.5",
        "minecraft": "~26.1",
        "java": ">=25",
        "fabric-api": "*"
    }
}
```

- [ ] **Step 3: 更新 settings.gradle**

添加 `include 'serverbot'`：

```groovy
include 'serverhelper'
include 'nocreepergrief'
include 'onlinemodefix'
include 'serverbot'
```

- [ ] **Step 4: 验证基础 Gradle 配置**

```bash
./gradlew --no-daemon --console=plain :serverbot:build
```

Expected: BUILD SUCCESSFUL（会失败因为没有主类，但 Gradle 配置正确即可）。

---

### Task 2: 创建 BotNetworkHandler

**Files:**
- Create: `serverbot/src/main/java/com/ysh/serverbot/network/BotNetworkHandler.java`

静默网络处理器。负责：
- 所有 inbound 包处理方法为空实现（不处理任何客户端发来的包）
- `tick()` 重置 keepalive 状态，防止 idle timeout 踢出
- `disconnect()` 为 no-op

```java
package com.ysh.serverbot.network;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class BotNetworkHandler extends ServerGamePacketListenerImpl {
    private static final long KEEPALIVE_INTERVAL = 15000L;

    public BotNetworkHandler(MinecraftServer server, ServerPlayer player, Connection connection) {
        super(server, player, connection);
    }

    @Override
    public void tick() {
        this.keepAliveTime = SystemUtils.getMillis() + KEEPALIVE_INTERVAL;
        this.keepAlivePending = false;
    }

    @Override
    public void disconnect(Component message) {
    }

    @Override
    public void onDisconnect(Component reason) {
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {
    }

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {
    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {
    }

    @Override
    public void handlePingRequest(ServerboundPingRequestPacket packet) {
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {
    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket packet) {
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {
    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {
    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {
    }

    @Override
    public void handleUseItemOnPacket(ServerboundUseItemOnPacket packet) {
    }

    @Override
    public void handleChat(ServerboundChatPacket packet) {
    }

    @Override
    public void handleChatCommand(ServerboundChatCommandPacket packet) {
    }

    @Override
    public void handleChatCommandSigned(ServerboundChatCommandSignedPacket packet) {
    }

    @Override
    public void handleChatAck(ServerboundChatAckPacket packet) {
    }

    @Override
    public void handleConfigurationFinished(ServerboundFinishConfigurationPacket packet) {
    }

    @Override
    public void handleCookieResponse(ServerboundCookieResponsePacket packet) {
    }

    @Override
    public void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket packet) {
    }
}
```

注意：`keepAliveTime` 和 `keepAlivePending` 需要检查是否是 `ServerGamePacketListenerImpl` 中的字段名。如果不是，可能需要调整。

- [ ] **Step 5: 编译检查 BotNetworkHandler**

```bash
./gradlew --no-daemon --console=plain :serverbot:compileJava
```

如果 `keepAliveTime`/`keepAlivePending` 字段不存在或不可访问，调整实现（可使用 Accessor Mixin 或完全跳过超类 tick 逻辑）。

---

### Task 3: 创建 ServerBotPlayer

**Files:**
- Create: `serverbot/src/main/java/com/ysh/serverbot/bot/ServerBotPlayer.java`

假人玩家实体。继承 `ServerPlayer`，标记 bot 身份。

```java
package com.ysh.serverbot.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerBotPlayer extends ServerPlayer {
    public ServerBotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation clientInfo) {
        super(server, level, profile, clientInfo);
    }

    public boolean isBot() {
        return true;
    }
}
```

- [ ] **Step 6: 编译检查**

```bash
./gradlew --no-daemon --console=plain :serverbot:compileJava
```

---

### Task 4: 创建 BotManager

**Files:**
- Create: `serverbot/src/main/java/com/ysh/serverbot/bot/BotManager.java`

假人生命周期管理器。单例模式。管理假人的创建、移除、列表。

```java
package com.ysh.serverbot.bot;

import com.mojang.authlib.GameProfile;
import com.ysh.serverbot.network.BotNetworkHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {
    private static final int MAX_BOTS = 5;
    private static final BotManager INSTANCE = new BotManager();

    private final Map<String, ServerBotPlayer> bots = new ConcurrentHashMap<>();

    private BotManager() {}

    public static BotManager getInstance() {
        return INSTANCE;
    }

    public ServerBotPlayer spawnBot(ServerPlayer owner) {
        if (bots.size() >= MAX_BOTS) {
            owner.sendSystemMessage(Component.literal("§c已达到最大假人数限制 (" + MAX_BOTS + " 个)"));
            return null;
        }

        String name = allocateName();
        if (name == null) {
            owner.sendSystemMessage(Component.literal("§c无法分配假人名称"));
            return null;
        }

        MinecraftServer server = owner.getServer();
        ServerLevel level = owner.serverLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        ServerBotPlayer bot = new ServerBotPlayer(server, level, profile, ClientInformation.createDefault());

        Connection connection = new Connection(PacketFlow.CLIENTBOUND) {
            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, PacketSendListener listener) {
            }

            @Override
            public void disconnect(Component message) {
            }
        };

        BotNetworkHandler handler = new BotNetworkHandler(server, bot, connection);
        bot.connection = handler;

        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false);
        server.getPlayerList().placeNewPlayer(connection, bot, cookie);

        bots.put(name, bot);
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("§7[ServerBot] 假人 §a" + name + " §7已上线"),
            false
        );

        return bot;
    }

    public boolean removeBot(String name) {
        ServerBotPlayer bot = bots.remove(name);
        if (bot == null) return false;

        MinecraftServer server = bot.getServer();
        if (server != null) {
            bot.connection.disconnect(Component.literal("Bot removed"));
            server.getPlayerList().remove(bot);
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§7[ServerBot] 假人 §e" + name + " §7已移除"),
                false
            );
        }
        return true;
    }

    public List<String> listBots() {
        return new ArrayList<>(bots.keySet());
    }

    public int getOnlineCount() {
        return bots.size();
    }

    public int getMaxBots() {
        return MAX_BOTS;
    }

    public void shutdown(MinecraftServer server) {
        for (String name : new HashSet<>(bots.keySet())) {
            removeBot(name);
        }
    }

    private String allocateName() {
        for (int i = 1; i <= MAX_BOTS; i++) {
            String name = String.format("Bot%02d", i);
            if (!bots.containsKey(name)) {
                return name;
            }
        }
        return null;
    }
}
```

编译器可能报的问题及修改方案：
- `bot.connection = handler` — `connection` 字段在 `ServerPlayer` 中超类可能是 `private`。需要 Mixin `@Accessor` 或反射。后面如果需要再加 Accessor。

- [ ] **Step 7: 编译检查 BotManager**

```bash
./gradlew --no-daemon --console=plain :serverbot:compileJava
```

如果 `bot.connection` 无法直接访问，创建 Accessor Mixin。
如果 `CommonListenerCookie` 构造函数不匹配，改用 `CommonListenerCookie.createInitial(profile, false)`。

---

### Task 5: 创建 BotCommand

**Files:**
- Create: `serverbot/src/main/java/com/ysh/serverbot/command/BotCommand.java`

```java
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
```

- [ ] **Step 8: 编译检查**

```bash
./gradlew --no-daemon --console=plain :serverbot:compileJava
```

---

### Task 6: 创建 ServerBotMod

**Files:**
- Create: `serverbot/src/main/java/com/ysh/serverbot/ServerBotMod.java`

模组入口点。

```java
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
```

- [ ] **Step 9: 编译检查**

```bash
./gradlew --no-daemon --console=plain :serverbot:compileJava
```

---

### Task 7: 全量构建验证

- [ ] **Step 10: 全量构建**

```bash
./gradlew --no-daemon --console=plain build
```

Expected: BUILD SUCCESSFUL

确认 JAR 产出：
```bash
Get-ChildItem serverbot/build/libs/*.jar
```
