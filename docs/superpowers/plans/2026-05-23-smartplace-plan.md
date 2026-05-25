# SmartPlace 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建 SmartPlace mod，让玩家无需对准方块特定面，根据面朝方向自动放置方块。

**Architecture:** 在 `ServerPlayerGameMode#useItemOn` 中通过 Mixin `@ModifyVariable` 拦截 `BlockHitResult` 参数，根据 `player.getLookAngle()` 重算 `Direction`，使用 `BlockHitResult.withDirection()` 创建新的 hit result。

**Tech Stack:** Fabric Loom, Minecraft 26.1 (Mojang mappings), Mixin

---

### Task 1: 项目脚手架

**Files:**
- Modify: `settings.gradle`
- Create: `smartplace/build.gradle`
- Create: `smartplace/src/main/resources/fabric.mod.json`
- Create: `smartplace/src/main/resources/smartplace.mixins.json`

- [ ] **Step 1: 创建 `smartplace/` 目录结构**

```bash
New-Item -ItemType Directory -Path "smartplace\src\main\java\com\ysh\smartplace\mixin" -Force
New-Item -ItemType Directory -Path "smartplace\src\main\resources" -Force
```

- [ ] **Step 2: 在 `settings.gradle` 中添加 `include 'smartplace'`**

```groovy
include 'serverhelper'
include 'nocreepergrief'
include 'smartplace'
```

- [ ] **Step 3: 创建 `smartplace/build.gradle`**

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

- [ ] **Step 4: 创建 `smartplace/src/main/resources/fabric.mod.json`**

```json
{
    "schemaVersion": 1,
    "id": "smartplace",
    "version": "${version}",
    "name": "SmartPlace",
    "description": "根据玩家面朝方向自动放置方块，无需精准对准特定面。",
    "authors": ["YSH"],
    "contact": {},
    "license": "MIT",
    "environment": "server",
    "entrypoints": {
        "main": [
            "com.ysh.smartplace.SmartPlaceMod"
        ]
    },
    "mixins": [
        "smartplace.mixins.json"
    ],
    "depends": {
        "fabricloader": ">=0.18.5",
        "minecraft": "~26.1",
        "java": ">=25",
        "fabric-api": "*"
    }
}
```

- [ ] **Step 5: 创建 `smartplace/src/main/resources/smartplace.mixins.json`**

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.ysh.smartplace.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "ServerPlayerGameModeMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

- [ ] **Step 6: Commit scaffolding**

```bash
git add settings.gradle smartplace/
git commit -m "feat: 添加 SmartPlace mod 项目脚手架"
```

---

### Task 2: 实现 DirectionHelper

**Files:**
- Create: `smartplace/src/main/java/com/ysh/smartplace/DirectionHelper.java`

- [ ] **Step 1: 创建 DirectionHelper 工具类**

```java
package com.ysh.smartplace;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class DirectionHelper {
    public static Direction fromLookAngle(Vec3 lookAngle) {
        double x = lookAngle.x;
        double y = lookAngle.y;
        double z = lookAngle.z;

        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);

        if (absY > absX && absY > absZ) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        } else if (absX > absZ) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add smartplace/src/main/java/com/ysh/smartplace/DirectionHelper.java
git commit -m "feat: 实现 DirectionHelper 工具类，根据视角向量计算方向"
```

---

### Task 3: 实现 SmartPlaceState

**Files:**
- Create: `smartplace/src/main/java/com/ysh/smartplace/SmartPlaceState.java`

- [ ] **Step 1: 创建 SmartPlaceState 状态管理器**

```java
package com.ysh.smartplace;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SmartPlaceState {
    private static final Map<UUID, Boolean> enabledPlayers = new ConcurrentHashMap<>();

    public static boolean isEnabled(ServerPlayer player) {
        return enabledPlayers.getOrDefault(player.getUUID(), false);
    }

    public static boolean toggle(ServerPlayer player) {
        boolean current = isEnabled(player);
        boolean newValue = !current;
        enabledPlayers.put(player.getUUID(), newValue);
        return newValue;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add smartplace/src/main/java/com/ysh/smartplace/SmartPlaceState.java
git commit -m "feat: 实现 SmartPlaceState 玩家状态管理器"
```

---

### Task 4: 实现 Mixin — ServerPlayerGameModeMixin

**Files:**
- Create: `smartplace/src/main/java/com/ysh/smartplace/mixin/ServerPlayerGameModeMixin.java`

- [ ] **Step 1: 创建 Mixin 类，拦截 `useItemOn` 中 `BlockHitResult` 参数**

```java
package com.ysh.smartplace.mixin;

import com.ysh.smartplace.DirectionHelper;
import com.ysh.smartplace.SmartPlaceState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerPlayer player;

    @ModifyVariable(
        method = "useItemOn",
        at = @At("HEAD"),
        argsOnly = true,
        index = 4
    )
    private BlockHitResult modifyDirection(BlockHitResult hitResult) {
        if (!SmartPlaceState.isEnabled(this.player)) {
            return hitResult;
        }
        return hitResult.withDirection(DirectionHelper.fromLookAngle(this.player.getLookAngle()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add smartplace/src/main/java/com/ysh/smartplace/mixin/ServerPlayerGameModeMixin.java
git commit -m "feat: 实现 ServerPlayerGameModeMixin，修改放置方向"
```

---

### Task 5: 实现 SmartPlaceMod 入口 + 命令注册

**Files:**
- Create: `smartplace/src/main/java/com/ysh/smartplace/SmartPlaceMod.java`

- [ ] **Step 1: 创建入口类，注册 `/smartplace` 命令**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add smartplace/src/main/java/com/ysh/smartplace/SmartPlaceMod.java
git commit -m "feat: 实现 SmartPlaceMod 入口和命令注册"
```

---

### Task 6: 更新 AGENTS.md

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 在 AGENTS.md 中添加 SmartPlace 模组说明**

在"## 子模组"表格中添加一行：
```
| SmartPlace | `smartplace/` | 智能方块放置，根据玩家面朝方向自动放置方块 |
```

在"## 添加新模组"部分前添加 SmartPlace 架构小节：
```
## SmartPlace 模组架构

- **入口**: `com.ysh.smartplace.SmartPlaceMod` (ModInitializer)
- **核心 Mixin** (`ServerPlayerGameModeMixin`): `@ModifyVariable` 拦截 `ServerPlayerGameMode#useItemOn()` 的参数 `BlockHitResult`
  - 根据 `player.getLookAngle()` 重新计算放置方向
  - 使用 `BlockHitResult.withDirection()` 创建修改后的 hit result
- **DirectionHelper**: 工具类，从视角向量确定 `Direction`
- **SmartPlaceState**: 每个玩家独立的开关状态（`ConcurrentHashMap<UUID, Boolean>`）
- **命令**: `/smartplace toggle|status`（默认关闭）
- **无配置文件**，功能全部内置于代码
```

- [ ] **Step 2: Commit**

```bash
git add AGENTS.md
git commit -m "docs: 更新 AGENTS.md 添加 SmartPlace 模组说明"
```

---

### Task 7: 构建验证

- [ ] **Step 1: 运行构建**

```bash
./gradlew --no-daemon --console=plain :smartplace:build
```

预期输出：`BUILD SUCCESSFUL`，JAR 文件生成在 `smartplace/build/libs/`。

- [ ] **Step 2: 验证 Mixin 应用正确**

检查构建日志中是否包含 Mixin 应用的 INFO 日志（带有 `SLF4J` 前缀的 SmartPlace 相关日志）。

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "chore: 构建验证通过"
```
