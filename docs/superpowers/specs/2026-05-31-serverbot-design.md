# ServerBot 假人模组设计文档

## 概述

Fabric 服务端假人模组，用于挂机不掉线。作为 `minecraft-mods` 父工程的新子模组，遵循现有模组架构风格。

## 核心需求

- 执行 `/bot` 命令在玩家位置生成一个假人实体
- 假人像普通玩家一样可见，但无交互
- 自动保持在线，不被 idle timeout 踢出
- 最大同时在线 5 个假人
- 任何人可执行命令
- 无配置文件

## 项目结构

```
serverbot/
├── build.gradle                  # fabric-loom 插件，同 serverhelper 模板
└── src/main/
    └── java/com/ysh/serverbot/
        ├── ServerBotMod.java          # ModInitializer 入口
        ├── bot/
        │   ├── BotManager.java        # 假人生命周期管理（单例）
        │   └── ServerBotPlayer.java   # 假人实体 extends ServerPlayer
        ├── command/
        │   └── BotCommand.java        # /bot 命令注册
        ├── network/
        │   └── BotNetworkHandler.java # 静默网络处理器，维护心跳防踢
```

## 配置

无配置文件。`max_bots = 5` 硬编码。

## 假人命名

自动生成：`Bot01`、`Bot02` … `Bot05`。BotManager 分配时取当前最小的可用编号。

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/bot spawn` | 在执行者位置生成一个假人 | 所有人 |
| `/bot remove <名字>` | 移除指定假人 | 所有人 |
| `/bot list` | 列出所有在线假人 | 所有人 |

## BotManager

单例管理器，维护 `Map<String, ServerBotPlayer>`。

关键方法：
- `spawnBot(ServerPlayer owner)` — 分配名称，创建 ServerBotPlayer，注册到 PlayerList
- `removeBot(String name)` — 断开并从世界移除
- `listBots()` — 返回在线假人列表
- `getOnlineCount()` — 当前在线数
- `shutdown()` — 服务端停止时清理

防踢核心：`BotNetworkHandler` 覆盖 idle timeout 相关逻辑，不做任何检查。

## ServerBotPlayer

继承 `ServerPlayer`：
- 构造时传入 BotNetworkHandler（非真实网络连接）
- 按正常玩家生命周期注册到 `PlayerList` 和 `ServerLevel`
- `tick()` 正常调用但无玩家输入处理

## BotNetworkHandler

实现/继承 `ServerGamePacketListenerImpl`：
- 所有 inbound 包处理方法为空实现
- 覆盖空闲超时检查方法，始终返回未超时
- 假 Connection 使用空通道

## 依赖

- Minecraft 26.1
- Fabric Loader >= 0.18.5
- Fabric API *
- Java >= 25
