# WebSocket 统一改造设计

## 背景

当前 `serverhelper` 模组与 NapCat 通信使用两套通道：
- **发消息**（QQNotifier, QQCommandHandler.sendToGroup）→ HTTP POST `/send_group_msg`
- **收指令**（QQCommandWSClient）→ WebSocket `/ws`

NapCat 的 OneBot v11 WebSocket 本身支持双向通信。统一为单一 WS 连接可简化架构和部署。

## 目标

- 删除 `java.net.http.HttpClient` 在模组中全部使用
- 一条 WS 连接同时负责收事件和发 API 请求
- `QQCommandServer.java` 已删除，不再涉及

## 架构

```
QQWSClient（独立 WS 传输层）
  ├── connect(api_url, token, mcServer)
  ├── disconnect()
  ├── sendAction(action, params) → CompletableFuture<JsonObject>
  └── on WS 文本帧 → 事件分发

事件分发:
  ├── "post_type": "message" → QQCommandHandler.handle()
  └── echo 响应 → 匹配并完成对应的 sendAction future

使用方（共享同一个 QQWSClient 实例）:
  ├── QQCommandHandler     ← 注册为事件监听, sendToGroup() 调用 sendAction
  └── QQNotifier           ← send() 内部调用 sendAction
```

## 组件细节

### QQWSClient（新增，替换 QQCommandWSClient）

- 包: `com.ysh.serverhelper.ws`
- 职责: 维护一条 WebSocket 连接，提供收发接口
- 关键 API:
  - `connect(config, mcServer)` — 连接到 NapCat WS，启动分发
  - `disconnect()` — 断开连接
  - `sendAction(action, params)` — 发送 OneBot 动作，返回 `CompletableFuture<JsonObject>`
- 断线自动重连（5s 间隔）
- 线程安全，`sendAction` 使用 `echo` 字段匹配请求-响应

### echo 机制

OneBot v11 支持在请求中附带 `echo` 字段，响应会原样返回：

```json
// 发送
{"action":"send_group_msg","params":{"group_id":xxx,"message":"xxx"},"echo":"req_1"}
// 响应
{"status":"ok","retcode":0,"data":{},"echo":"req_1"}
```

`sendAction` 内部:
1. 生成唯一 echo ID（AtomicLong 计数器）
2. 将 echo → CompletableFuture 存入 `ConcurrentHashMap<String, CompletableFuture<JsonObject>>`
3. 发送 WS 帧
4. 事件接收线程收到响应时，匹配 echo，完成 future（超时 10s）
5. 超时或断线时异常完成并从 map 中移除
6. `disconnect()` 时遍历 map 异常完成所有待处理的 future

### QQCommandHandler（修改）

- 保持静态类设计，`init(mcServer, wsClient)` 新增 `QQWSClient` 参数
- 删除 `HttpClient client` 静态字段
- `sendToGroup(groupId, text)` 删除 `config` 参数，改为调用 `wsClient.sendAction("send_group_msg", ...)`
- 不再需要 `token` 和 `api_url`，这些在 `QQWSClient.connect()` 时已提供

### QQNotifier（修改）

- 构造器改为 `QQNotifier(QQConfig config, QQWSClient wsClient)`
- 删除 `HttpClient client` 字段
- `send()` 内部改为调用 `wsClient.sendAction("send_group_msg", ...)`
- 删除 `escapeJson`（由 Gson/JsonObject 序列化替代）

### ServerHelperMod（修改）

- 创建一个 `QQWSClient` 单例实例
- `SERVER_STARTING` 时调用 `connect()`
- `SERVER_STOPPING` 时调用 `disconnect()`
- 将 `QQWSClient` 实例注入 `QQCommandHandler` 和 `QQNotifier`

## 配置变更

- 删除 `callback_port`（已完成）
- `api_url` 语义变为 WS 地址来源（自动推导 `ws://` 或 `wss://`）+ 路径拼接

## 部署变更

- OpenResty 只需暴露 `/ws` 路径到 WS 端口 3001
- 不再需要 HTTP 3000 端口通过隧道（但保留不影响）

## 不回退的项

- `Notifier` 接口不变
- 各事件处理器（PlayerJoinHandler 等）不变
- 配置格式不变（只删了 `callback_port`）
- ModConfig 中的字段不变
