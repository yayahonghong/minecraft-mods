# OnlineModeFix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建一个 Fabric 服务端 Mod，允许 keep `online-mode=true` 的同时自动放行离线玩家。

**Architecture:** Mixin 拦截 `ServerLoginPacketListenerImpl.disconnect()`，当 `state == AUTHENTICATING` 时（即认证失败），跳过断开连接，转为使用离线 `GameProfile` 放行玩家。

**Tech Stack:** Java 25, Fabric Loader 0.19.2, Minecraft 26.1, Mixin

---

### Task 1: 项目脚手架 — 注册子模块 + build.gradle + 资源文件

**Files:**
- Modify: `settings.gradle`
- Create: `onlinemodefix/build.gradle`
- Create: `onlinemodefix/src/main/resources/fabric.mod.json`
- Create: `onlinemodefix/src/main/resources/onlinemodefix.mixins.json`

- [ ] **Step 1: settings.gradle — 注册子模块**

在 `settings.gradle` 末尾添加:
```groovy
include 'onlinemodefix'
```

- [ ] **Step 2: onlinemodefix/build.gradle**

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

- [ ] **Step 3: onlinemodefix/src/main/resources/fabric.mod.json**

```json
{
    "schemaVersion": 1,
    "id": "onlinemodefix",
    "version": "${version}",
    "name": "OnlineModeFix",
    "description": "Allows offline players to join while online-mode=true.",
    "authors": ["YSH"],
    "contact": {},
    "license": "MIT",
    "environment": "server",
    "entrypoints": {
        "main": [
            "com.ysh.onlinemodefix.OnlineModeFixMod"
        ]
    },
    "mixins": [
        "onlinemodefix.mixins.json"
    ],
    "depends": {
        "fabricloader": ">=0.18.5",
        "minecraft": "~26.1",
        "java": ">=25"
    }
}
```

- [ ] **Step 4: onlinemodefix/src/main/resources/onlinemodefix.mixins.json**

```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "com.ysh.onlinemodefix.mixin",
    "compatibilityLevel": "JAVA_21",
    "mixins": [
        "ServerLoginMixin"
    ],
    "injectors": {
        "defaultRequire": 1
    }
}
```

### Task 2: 入口类 + Mixin

**Files:**
- Create: `onlinemodefix/src/main/java/com/ysh/onlinemodefix/OnlineModeFixMod.java`
- Create: `onlinemodefix/src/main/java/com/ysh/onlinemodefix/mixin/ServerLoginMixin.java`

- [ ] **Step 1: OnlineModeFixMod.java**

```java
package com.ysh.onlinemodefix;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlineModeFixMod implements ModInitializer {
    public static final String MOD_ID = "onlinemodefix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("OnlineModeFix loaded — offline players will be allowed when online-mode=true");
    }
}
```

- [ ] **Step 2: ServerLoginMixin.java**

```java
package com.ysh.onlinemodefix.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.UUIDUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginMixin {

    @Shadow @Final private static Logger LOGGER;

    @Shadow private volatile ServerLoginPacketListenerImpl.State state;

    @Shadow private String requestedUsername;

    @Shadow private void startClientVerification(GameProfile profile);

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(Component reason, CallbackInfo ci) {
        if (this.state == ServerLoginPacketListenerImpl.State.AUTHENTICATING) {
            GameProfile offlineProfile = UUIDUtil.createOfflineProfile(requestedUsername);
            LOGGER.info("{} logged in as offline player (online auth failed)", requestedUsername);
            this.startClientVerification(offlineProfile);
            ci.cancel();
        }
    }
}
```

Note: `@Shadow` fields must be declared with the exact same name as in the target class. The `state` field is `volatile`, `requestedUsername` is a plain `String`, and `LOGGER` is `static final`.

### Task 3: 构建验证

**Files:** (no file changes)
- Run: `./gradlew --no-daemon --console=plain :onlinemodefix:build`

- [ ] **Step 1: 构建**

Run:
```bash
cd E:\Desktop\minecraft-mods
.\gradlew --no-daemon --console=plain :onlinemodefix:build
```

Expected: BUILD SUCCESSFUL. JAR 产出在 `onlinemodefix/build/libs/onlinemodefix-1.0.0.jar`

