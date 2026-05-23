# Multi-Mod Project Restructure Design

## Objective

Convert the single-mod Fabric project `serverhelper` into a multi-module Gradle parent project `minecraft-mods`, where the existing `serverhelper` mod becomes a submodule and future mods can be added as siblings.

## Target Directory Structure

```
minecraft-mods/                          (parent root)
├── build.gradle                         (common config via subprojects {})
├── settings.gradle                      (rootProject.name + include submodules)
├── gradle.properties                    (shared version properties)
├── .gitignore
├── README.md
├── gradle/wrapper/
├── gradlew / gradlew.bat
├── run/                                 (Minecraft dev runtime, stays at root)
│
├── serverhelper/                        (existing mod as submodule)
│   ├── build.gradle                     (loom plugin + mod-specific config)
│   ├── src/main/
│   ├── src/test/
│   ├── config/                          (moved from root config/)
│   └── .gitkeep
│
└── mod2/                                (future mod, same pattern)
    ├── build.gradle
    └── src/
```

## Build Configuration

### Root `build.gradle`

- No plugins (Loom is applied only in submodules)
- `subprojects {}` block provides common Java config:
  - `apply plugin: 'java'`
  - Maven Central repository
  - JUnit 5 test dependencies
  - Java 25 compile options
  - withSourcesJar()
- No Minecraft dependencies at root level

### Root `settings.gradle`

- `pluginManagement {}` with Fabric maven + Gradle Plugin Portal
- Plugin version resolution via `plugins {}` block (avoids repeating version in each submodule)
- `rootProject.name = 'minecraft-mods'`
- `include 'serverhelper'`

### Submodule `serverhelper/build.gradle`

- Applies `net.fabricmc.fabric-loom` (version resolved from root pluginManagement)
- Applies `maven-publish`
- Declares version/group from root `gradle.properties`
- Dependencies: minecraft, fabric-loader, fabric-api
- processResources for fabric.mod.json expansion
- jar task with LICENSE handling

### `gradle.properties` (root)

Unchanged. Shared properties accessible by all submodules:
- minecraft_version, loader_version, loom_version
- mod_version, maven_group
- fabric_api_version

## File Migration Plan

| Current Path | Target Path | Notes |
|---|---|---|
| `build.gradle` | → root `build.gradle` (new) | Rewritten for multi-module |
| | → `serverhelper/build.gradle` (new) | Mod-specific config |
| `settings.gradle` | → root `settings.gradle` | Updated rootProject.name + include |
| `gradle.properties` | → root (unchanged) | |
| `src/main/` | → `serverhelper/src/main/` | Move entire tree |
| `src/test/` | → `serverhelper/src/test/` | Move entire tree |
| `config/` | → `serverhelper/config/` | Move entire tree |
| `README.md` | → root `README.md` | Update to describe parent project |
| `.gitignore` | → root `.gitignore` | Update config path |
| `gradle/wrapper/` | → root (unchanged) | |
| `gradlew` | → root (unchanged) | |
| `gradlew.bat` | → root (unchanged) | |
| `run/` | → root (unchanged) | Loom expects runtime at project root |

## Adding New Mods

To add a new mod:
1. Create `newmod/build.gradle` using serverhelper's build.gradle as template
2. Add `include 'newmod'` to `settings.gradle`
3. Create `newmod/src/main/` with fabric.mod.json and entrypoint

## Git History

The repo has only 1 commit, so history preservation is trivial. The directory rename and file moves will be done in a single commit.
