# ServerHelper Mod

一个 Fabric 服务端 Mod，提供多事件 QQ 通知推送 + QQ 远程管理命令。

## 功能

- **事件通知** — 8 种事件实时推送到 QQ 群：加入、退出、死亡（汉化）、成就（汉化）、聊天、服务器启停、OP 变更
- **QQ 远程命令** — 在 QQ 群发 `#list`、`#tps`、`#status`、`#say`、`#cmd` 管理服务器
- **游戏内管理** — `/helper list`、`/helper toggle <event>`、`/helper test`、`/helper reload`
- **原生中文支持** — 模组内置官方 `zh_cn.json`，成就/死亡消息均为中文
## 架构

```
Minecraft Server ───WebSocket（双向）───→ NapCat WS Server ←→ QQ 群
                    ├── 发送通知
                    └── 接收指令
```

## 环境要求

- Minecraft 26.1, Fabric Loader >= 0.18.5, Fabric API, Java >= 25
- NapCatQQ (v4.18+) 启用 WebSocket 服务器

## 配置

文件路径: `config/serverhelper.json`

```json
{
  "qq": {
    "enabled": true,
    "api_url": "http://localhost:3000",
    "token": "",
    "group_id": 0,
    "command_prefix": "#",
    "admin_qq": []
  },
  "events": {
    "join": { "enabled": true, "message": "🟢 {player} 加入了游戏" },
    "quit": { "enabled": true, "message": "🔴 {player} 退出了游戏" },
    "death": { "enabled": true, "message": "💀 {death_message}" },
    "advancement": { "enabled": true, "message": "🏆 {player} 获得了成就 {advancement}" },
    "chat": { "enabled": false, "message": "💬 {player}: {message}" },
    "server_start": { "enabled": true, "message": "✅ 服务器已启动" },
    "server_stop": { "enabled": true, "message": "🛑 服务器即将关闭" },
    "op_change": { "enabled": true, "message": "👑 {player} 的 OP 状态已变更为 {status}" }
  },
  "excluded_players": []
}
```

### 模板变量

| 变量 | 通用 | death | advancement | chat | op_change |
|---|---|---|---|---|---|
| `{player}` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `{uuid}` | ✅ | | | | |
| `{ip}` | ✅ | | | | |
| `{client}` | ✅ | | | | |
| `{time}` | ✅ | ✅ | ✅ | ✅ | |
| `{death_message}` | | ✅ | | | |
| `{advancement}` | | | ✅ | | |
| `{message}` | | | | ✅ | |
| `{status}` | | | | | ✅ |

## NapCat 配置

### WebSocket 服务器

```json
{
  "network": {
    "wsServers": [{
      "name": "serverhelper-ws",
      "enable": true,
      "port": 3001,
      "host": "0.0.0.0",
      "token": "<你的Token>"
    }]
  }
}
```

## QQ 命令

| 指令 | 说明 | 权限 |
|---|---|---|
| `#help` | 列出可用指令 | 所有人 |
| `#list` | 在线玩家列表 | 所有人 |
| `#tps` | 服务器 TPS | 所有人 |
| `#status` | 服务器状态 (TPS/内存/在线/Uptime) | 所有人 |
| `#say <msg>` | 广播消息 | admin_qq |
| `#cmd <command>` | 执行任意命令 | admin_qq |

## 游戏内命令

| 命令 | 说明 |
|---|---|
| `/helper list` | 查看所有事件和 QQ 配置状态 |
| `/helper test` | 发送测试通知 |
| `/helper reload` | 热重载配置 |
| `/helper toggle <event>` | 开关指定事件或 qq 总开关 |

## 构建

```bash
./gradlew build
# 输出: build/libs/serverhelper-1.0.0.jar
```
