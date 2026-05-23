# SmartPlace — 智能方块放置 Mod

## 概述

服务端 Fabric mod，当玩家右击放置方块时，根据玩家面朝方向自动判断放置位置，无需精确对准方块的某个面。

## 运行端

**服务端 only** — 无需客户端安装，所有玩家自动生效。

## 切换控制

`/smartplace toggle` 命令，每个玩家独立开关，默认关闭。

## 架构

| 组件 | 说明 |
|---|---|
| `SmartPlaceMod` | Mod 入口，实现 `ModInitializer`，注册 `/smartplace` 命令 |
| `ServerGamePacketListenerImplMixin` | 核心 Mixin，修改 `handleUseItemOn` 中 `BlockHitResult` 的 `Direction` |
| `DirectionHelper` | 工具类，根据 `player.getLookAngle()` 计算目标方向 |
| `SmartPlaceState` | 管理每个玩家的 toggle 状态，使用 `PlayerAttachment` 或 `Map<UUID, Boolean>` |

## 核心逻辑

```
玩家右击 → 客户端发包 (ServerboundUseItemOnPacket)
  → 服务端 ServerGamePacketListenerImpl#handleUseItemOn
  → Mixin 拦截，在 useOn 调用前修改 BlockHitResult
    → 用 DirectionHelper 根据 Player.getLookAngle() 计算新 Direction
    → 构造新的 BlockHitResult(newDirection, blockPos, location, inside)
  → 服务端按修改后的 Direction 放置方块
```

## Direction 计算规则

- 取 `player.getLookAngle()` 的归一化向量
- 取绝对值最大的轴分量（|x|, |y|, |z|）作为主方向
- 根据该分量正负确定 `Direction.UP/DOWN/EAST/WEST/SOUTH/NORTH`
- 俯角 > 45° 优先判断为 DOWN，仰角 > 45° 为 UP

## 命令

```
/smartplace toggle    — 开启/关闭智能放置（默认关闭）
/smartplace status    — 查看当前状态
```

## 文件结构

```
smartplace/
├── build.gradle
├── src/main/
│   ├── java/com/ysh/smartplace/
│   │   ├── SmartPlaceMod.java
│   │   ├── SmartPlaceState.java
│   │   ├── DirectionHelper.java
│   │   └── mixin/
│   │       └── ServerGamePacketListenerImplMixin.java
│   └── resources/
│       ├── fabric.mod.json
│       └── smartplace.mixins.json
```

## 依赖

- Fabric API（命令注册）
- 无外部配置/依赖

## 构建

```
./gradlew --no-daemon --console=plain :smartplace:build
```
