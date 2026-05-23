# Multi-Mod Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the single-mod project into a multi-module Gradle parent project with `serverhelper` as a submodule.

**Architecture:** Parent `minecraft-mods` holds common config (`subprojects {}` in root `build.gradle`). Each mod is a subdirectory with its own `build.gradle` applying `fabric-loom`. Plugin versions resolved via root `settings.gradle`'s `pluginManagement {}`. Source/config files move under `serverhelper/`.

**Tech Stack:** Gradle multi-project, Fabric Loom

---

### Task 1: Create `serverhelper/` directory structure

**Files:**
- Create: `serverhelper/` directory
- Create: `serverhelper/src/` (move target)
- Create: `serverhelper/config/` (move target)

- [ ] **Step 1: Create the target directories**

```powershell
New-Item -ItemType Directory -Path "E:\Desktop\serverhelper\serverhelper\src" -Force
New-Item -ItemType Directory -Path "E:\Desktop\serverhelper\serverhelper\config" -Force
```

---

### Task 2: Move `src/` into `serverhelper/`

**Files:**
- Move: `src/main/` → `serverhelper/src/main/`
- Move: `src/test/` → `serverhelper/src/test/`

- [ ] **Step 1: Move the source tree**

```powershell
Move-Item -LiteralPath "E:\Desktop\serverhelper\src\main" -Destination "E:\Desktop\serverhelper\serverhelper\src\main"
Move-Item -LiteralPath "E:\Desktop\serverhelper\src\test" -Destination "E:\Desktop\serverhelper\serverhelper\src\test"
```

- [ ] **Step 2: Remove empty old `src/`**

```powershell
Remove-Item -LiteralPath "E:\Desktop\serverhelper\src" -Force
```

---

### Task 3: Move `config/` into `serverhelper/`

**Files:**
- Move: `config/serverhelper.json` → `serverhelper/config/serverhelper.json`
- Move: `config/serverhelper.json.example` → `serverhelper/config/serverhelper.json.example`

- [ ] **Step 1: Move config files**

```powershell
Move-Item -LiteralPath "E:\Desktop\serverhelper\config\serverhelper.json" -Destination "E:\Desktop\serverhelper\serverhelper\config\serverhelper.json"
Move-Item -LiteralPath "E:\Desktop\serverhelper\config\serverhelper.json.example" -Destination "E:\Desktop\serverhelper\serverhelper\config\serverhelper.json.example"
```

- [ ] **Step 2: Remove empty old `config/`**

```powershell
Remove-Item -LiteralPath "E:\Desktop\serverhelper\config" -Force
```

---

### Task 4: Create `serverhelper/build.gradle`

**Files:**
- Create: `serverhelper/build.gradle`

- [ ] **Step 1: Write mod-specific build file**

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

### Task 5: Rewrite root `build.gradle`

**Files:**
- Modify: `build.gradle` (root)

- [ ] **Step 1: Write root build.gradle with subprojects config**

```groovy
subprojects {
    apply plugin: 'java'

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation "org.junit.jupiter:junit-jupiter:5.10.2"
        testRuntimeOnly "org.junit.platform:junit-platform-launcher"
    }

    test {
        useJUnitPlatform()
    }

    tasks.withType(JavaCompile).configureEach {
        it.options.release = 25
    }

    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
```

---

### Task 6: Update `settings.gradle`

**Files:**
- Modify: `settings.gradle` (root)

- [ ] **Step 1: Rewrite settings.gradle**

```groovy
pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id 'net.fabricmc.fabric-loom' version "${loom_version}"
    }
}

rootProject.name = 'minecraft-mods'

include 'serverhelper'
```

---

### Task 7: Update `.gitignore`

**Files:**
- Modify: `.gitignore` (root)

- [ ] **Step 1: Update config path in .gitignore**

Change line 29 from `config/serverhelper.json` to `serverhelper/config/serverhelper.json`:

```powershell
# Update the gitignore path for mod config
```

Use edit tool to change line 29 in `.gitignore`:

Old:
```
config/serverhelper.json
```
New:
```
serverhelper/config/serverhelper.json
```

---

### Task 8: Move old `README.md` into `serverhelper/` and clean up

**Files:**
- Move: `README.md` → `serverhelper/README.md`
- Delete: `serverhelper/src/main/resources/assets/`

- [ ] **Step 1: Move project-specific README to mod level**

```powershell
Move-Item -LiteralPath "E:\Desktop\serverhelper\README.md" -Destination "E:\Desktop\serverhelper\serverhelper\README.md"
```

- [ ] **Step 2: Update serverhelper/README.md title (line 1)**

Edit line 1 from `# ServerHelper` to `# ServerHelper Mod`.

- [ ] **Step 3: Remove empty asset directory**

```powershell
Remove-Item -LiteralPath "E:\Desktop\serverhelper\serverhelper\src\main\resources\assets" -Recurse -Force
```

---

### Task 9: Create root `README.md`

**Files:**
- Create: `README.md` (root)

- [ ] **Step 1: Write parent project README**

```markdown
# Minecraft Mods

多模组 Fabric 项目父工程。

## 子模组

| 模组 | 目录 | 说明 |
|---|---|---|
| ServerHelper | `serverhelper/` | 服务器管理模组，集成 QQ 通知与远程命令 |

## 构建

```bash
./gradlew build
```

各模组 JAR 输出在对应子目录的 `build/libs/` 下。

## 环境要求

- Minecraft 26.1, Fabric Loader >= 0.18.5, Java >= 25
```

---

---

### Task 10: Commit everything

- [ ] **Step 1: Check git status and commit**

```powershell
git status
git add -A
git commit -m "refactor: restructure project into multi-module Gradle parent

- Rename project to minecraft-mods (multi-module parent)
- Move src/ into serverhelper/ subdirectory
- Move config/ into serverhelper/ subdirectory
- Create root build.gradle with subprojects common config
- Create serverhelper/build.gradle with mod-specific loom config
- Update settings.gradle with include 'serverhelper'
- Clean up empty asset directory"
```

---

### Task 11: Rename parent directory and verify

- [ ] **Step 1: Rename the parent directory**

```powershell
Rename-Item -LiteralPath "E:\Desktop\serverhelper" -NewName "E:\Desktop\minecraft-mods"
```

- [ ] **Step 2: Verify git still works**

```powershell
Set-Location -LiteralPath "E:\Desktop\minecraft-mods"
git status
git log --oneline -1
```

Expected: clean working tree, same commit history.

- [ ] **Step 3: Verify Gradle project loads**

```powershell
.\gradlew projects
```

Expected: Shows root project 'minecraft-mods' with subproject ':serverhelper'
