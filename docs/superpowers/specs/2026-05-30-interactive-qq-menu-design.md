# 交互式 QQ 菜单设计文档

## 概述

为 ServerHelper 模组的 QQ 机器人增加**文字菜单**交互功能。用户在群内发送 `#menu` 后看到编号列表，回复编号即可执行对应命令。

> **背景**：NapCat 的 OneBot v11 实现不支持 `send_group_msg` 的 `keyboard` 参数（其 TypeBox schema 无 keyboard 字段），因此改用文字菜单 + 回复编号的方案。

## 交互流程

```
用户发送 "#menu"
    │
QQCommandHandler 捕获
    │
创建 MenuSession（记录用户状态，60 秒超时）
    │
发送文字菜单（编号列表）
    │
用户回复 "1"、"2" 等编号
    │
QQCommandHandler 检测到用户有活跃会话
    │
解析编号 → 执行对应命令 → 回复结果
```

## 文字菜单格式

```
🏠 MC 服务器菜单
回复编号执行命令，回复 #cancel 取消

[1] 👥 在线玩家
[2] 📊 服务器状态
[3] 🎮 服务器延迟
[4] 🔄 刷新菜单
```

## MenuSession 管理

- **`ConcurrentHashMap<Long, MenuSession>`** 按 QQ 用户 ID 存储
- 60 秒自动过期（`Instant.now() + 60s`），查询时检测并清理
- `#cancel` 命令主动清除会话
- 选择菜单项后自动清除会话（"刷新菜单"除外，会重置新会话）

## 代码改动

### 新增文件

**`qqcmd/MenuSession.java`**
- 状态类：`userId`、`groupId`、`items`、`createdAt`
- 静态方法：`hasActive()`、`get()`、`set()`、`remove()`
- `MenuItem` 内部 record：`label`（显示文本）、`action`（执行命令）

### 修改文件

**`ws/QQWSClient.java`**
- 回退：删除 `sendAction(String, JsonObject, JsonObject)` keyboard 重载

### 删除文件

**`ws/KeyboardBuilder.java`**
- 已删除（NapCat 不支持 keyboard 参数，该代码无效）

**`qqcmd/QQCommandHandler.java`**
- 重写 `handle`：
  - `#menu`/`#help` → 创建 MenuSession + 发送文字菜单
  - `#cancel` → 清除活跃会话
  - 首次解析：优先检查命令前缀，否则检查 MenuSession
- 移除 `KeyboardBuilder` 依赖
- 移除 `sendToGroup(groupId, text, keyboard)` 重载
- 新增 `buildMenuText()` + `handleMenuChoice()`

## 边界情况

- 用户回复非数字 → 提示"请输入有效编号"
- 编号超出范围 → 提示"无效选项（请输入 1-N）"
- 会话超时（60 秒） → 静默忽略回复
- 用户未发 `#menu` 直接回复数字 → 忽略（无会话）
- `#cancel` 时无活跃会话 → 静默忽略
- WebSocket 未连接 → `sendAction` 返回异常 CompletableFuture，无回复
