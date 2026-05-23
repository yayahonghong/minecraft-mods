# AGENTS.md — Minecraft Mods

语言：中文。所有注释、提交信息、文档优先使用中文。

## 项目结构

多模组 Fabric 父工程，当前包含子模组 `serverhelper` 和 `nocreepergrief`。

```
minecraft-mods/
├── build.gradle              # 子模块公共配置（Java 25, JUnit 5, sourcesJar）
├── settings.gradle           # pluginManagement + include 'serverhelper'
├── gradle.properties         # loam/fabric/loader/minecraft 版本号
├── serverhelper/
│   ├── build.gradle          # fabric-loom 插件, minecraft/fabric-api 依赖
│   ├── src/main/java/        # 模组代码
│   ├── src/test/java/        # JUnit 5 测试
│   └── config/               # 示例配置 + gitignored 的真实凭据
├── nocreepergrief/
│   ├── build.gradle          # fabric-loom 插件, minecraft/fabric-api 依赖
│   ├── src/main/java/        # 模组代码（3 个 Mixin 文件）
│   └── src/main/resources/   # fabric.mod.json + mixins.json
└── run/                      # Minecraft 开发运行时（Loom 期望在根目录）
```

## 子模组

| 模组 | 路径 | 功能 |
|---|---|---|
| ServerHelper | `serverhelper/` | QQ 通知、命令、服务器管理 |
| NoCreeperGrief | `nocreepergrief/` | 阻止苦力怕爆炸破坏地形，视觉改为真实烟花火箭效果 |

## 构建与测试

- **全量构建**: `./gradlew build`（JAR 产出在 `serverhelper/build/libs/`）
- **跑测试**: `./gradlew test`（JUnit 5, 无集成测试依赖）
- **跑单个测试**: `./gradlew :serverhelper:test --tests "com.ysh.serverhelper.config.ModConfigManagerTest"`
- **Java 版本**: 必须 >= 25，编译 `--release 25`
- **Gradle**: 9.5.0, `org.gradle.parallel=true`, `org.gradle.configuration-cache=false`
- **可靠构建命令（避免 daemon 导致进程不退出）**: `./gradlew --no-daemon --console=plain :子模块名:build`

## 配置

- 运行时配置路径: **`config/serverhelper.json`**（相对于工作目录，非 `serverhelper/config/`）
- 仓库中的凭据文件: `serverhelper/config/serverhelper.json`（已被 `.gitignore` 排除）
- 示例配置: `serverhelper/config/serverhelper.json.example`
- Mod 使用 Gson 序列化/反序列化配置（`ModConfig` + `ModConfigManager`）
- 事件类型使用 `LinkedHashMap` 保证顺序

## 模组架构

- **入口**: `com.ysh.serverhelper.ServerHelperMod` (ModInitializer)
- **事件处理器**: 每个事件一个独立 Handler 类（`PlayerJoinHandler`, `PlayerDeathHandler` 等），通过静态 `register()` 方法注册
- **QQ 通知**: `Notifier` + `QQNotifier` 发送 HTTP 请求到 NapCat
- **QQ 命令**: `QQCommandServer` 监听回调端口 + `QQCommandHandler` 处理指令
- **Mixin**: `PlayerAdvancementsMixin` — 成就监听通过 Mixin 实现
- **国际化**: `ServerI18n` 加载 `zh_cn.json` 将死讯/成就翻译为中文
- **命令**: `/helper list|toggle|test|reload` 通过 Fabric API Command API 注册

## NoCreeperGrief 模组架构

- **入口**: `com.ysh.nocreepergrief.NoCreeperGriefMod` (ModInitializer)
- **核心 Mixin** (`CreeperExplosionMixin`): `@Redirect` 拦截 `ServerLevel.explode()`，阻止地形破坏
  - 手动计算距离衰减伤害
  - 生成真实 `FireworkRocketEntity`（1 tick 后自动爆炸）展现原版烟花彩色效果
  - 两段烟花：红/金/白大球 + 青/紫/绿爆裂，带拖尾闪烁
- **Accessor** (`FireworkRocketEntityAccessor`): 访问 `life`/`lifetime` 私有字段
- **伤害取消** (`FireworkRocketEntityMixin`): 拦截 `dealExplosionDamage`，避免烟花叠加伤害
- **无配置文件**，功能全部内置于代码

## 添加新模组

1. 复制 `serverhelper/build.gradle` 作为模板
2. 在 `settings.gradle` 添加 `include 'newmod'`
3. 创建 `newmod/src/main/` 并编写 `fabric.mod.json` + 入口类

## 关键依赖版本

| 组件 | 版本 |
|---|---|
| Minecraft | 26.1 |
| Fabric Loader | >= 0.18.5（实际 0.19.2） |
| Fabric API | 0.149.0+26.1.2 |
| Fabric Loom | 1.16-SNAPSHOT |
| Java | >= 25 |
