# NoCreeperGrief Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a new Fabric mod `nocreepergrief` that replaces creeper explosions with firework visuals and prevents terrain damage.

**Architecture:** New submodule `nocreepergrief` under the multi-module Gradle parent. A single Mixin into `Creeper.explodeCreeper()` cancels the default explosion, calls `createExplosion(..., KEEP)` for entity damage + sound, and spawns `ParticleTypes.FIREWORK` particles. No configuration, no event handlers.

**Tech Stack:** Fabric Loom 1.16-SNAPSHOT, Mojmap mappings, Minecraft 26.1, Java 25, Mixin 0.8+

---

### Task 1: Create submodule directory structure

**Files:**
- Create: `nocreepergrief/` directory tree

- [ ] **Step 1: Create directories**

```powershell
New-Item -ItemType Directory -Path "E:\Desktop\minecraft-mods\nocreepergrief\src\main\java\com\ysh\nocreepergrief\mixin" -Force
New-Item -ItemType Directory -Path "E:\Desktop\minecraft-mods\nocreepergrief\src\main\resources" -Force
New-Item -ItemType Directory -Path "E:\Desktop\minecraft-mods\nocreepergrief\config" -Force
```

---

### Task 2: Create `nocreepergrief/build.gradle`

**Files:**
- Create: `nocreepergrief/build.gradle`

- [ ] **Step 1: Write build.gradle**

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

---

### Task 3: Create `fabric.mod.json`

**Files:**
- Create: `nocreepergrief/src/main/resources/fabric.mod.json`

- [ ] **Step 1: Write fabric.mod.json**

```json
{
    "schemaVersion": 1,
    "id": "nocreepergrief",
    "version": "${version}",
    "name": "NoCreeperGrief",
    "description": "Prevents creeper explosions from destroying terrain; replaces visual with firework particles.",
    "authors": ["YSH"],
    "contact": {},
    "license": "MIT",
    "environment": "server",
    "entrypoints": {
        "main": [
            "com.ysh.nocreepergrief.NoCreeperGriefMod"
        ]
    },
    "mixins": [
        "nocreepergrief.mixins.json"
    ],
    "depends": {
        "fabricloader": ">=0.18.5",
        "minecraft": "~26.1",
        "java": ">=25",
        "fabric-api": "*"
    }
}
```

---

### Task 4: Create mixin config

**Files:**
- Create: `nocreepergrief/src/main/resources/nocreepergrief.mixins.json`

- [ ] **Step 1: Write mixins.json**

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.ysh.nocreepergrief.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "CreeperExplosionMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

---

### Task 5: Create `NoCreeperGriefMod.java` entrypoint

**Files:**
- Create: `nocreepergrief/src/main/java/com/ysh/nocreepergrief/NoCreeperGriefMod.java`

- [ ] **Step 1: Write entrypoint class**

```java
package com.ysh.nocreepergrief;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoCreeperGriefMod implements ModInitializer {
    public static final String MOD_ID = "nocreepergrief";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("NoCreeperGrief loaded — creeper terrain destruction disabled");
    }
}
```

---

### Task 6: Create `CreeperExplosionMixin.java`

**Files:**
- Create: `nocreepergrief/src/main/java/com/ysh/nocreepergrief/mixin/CreeperExplosionMixin.java`

- [ ] **Step 1: Write mixin class**

```java
package com.ysh.nocreepergrief.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public class CreeperExplosionMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void onExplodeCreeper(CallbackInfo ci) {
        Creeper self = (Creeper)(Object)this;
        if (self.level().isClientSide()) return;

        ServerLevel world = (ServerLevel)self.level();
        float radius = self.isPowered() ? 6.0f : 3.0f;

        self.dead = true;

        world.explode(self, self.getX(), self.getY(), self.getZ(), radius,
            Explosion.DestructionType.KEEP);

        world.sendParticles(ParticleTypes.FIREWORK,
            self.getX(), self.getY(), self.getZ(),
            50, 2.0, 2.0, 2.0, 0.1);

        self.discard();
        ci.cancel();
    }
}
```

---

### Task 7: Add to `settings.gradle`

**Files:**
- Modify: `settings.gradle` (line 16)

- [ ] **Step 1: Insert `include 'nocreepergrief'`**

Edit `settings.gradle` to add the new submodule after `serverhelper`:

```
include 'serverhelper'
include 'nocreepergrief'
```

---

### Task 8: Update `AGENTS.md`

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Add nocreepergrief to project structure and mod list**

Add a row to the 子模组 table and optionally note the new mod under architecture.

---

### Task 9: Build and verify

- [ ] **Step 1: Build the project**

```powershell
./gradlew build
```

Expected: BUILD SUCCESSFUL. JAR at `nocreepergrief/build/libs/nocreepergrief-1.0.0.jar`

- [ ] **Step 2: Run existing tests**

```powershell
./gradlew test
```

Expected: All tests pass (ModConfigManagerTest)

- [ ] **Step 3: Verify jar contents**

```powershell
jar tf nocreepergrief/build/libs/nocreepergrief-1.0.0.jar
```

Expected: Contains mixins, mod class, fabric.mod.json
