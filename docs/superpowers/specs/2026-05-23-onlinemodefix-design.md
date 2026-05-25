# OnlineModeFix 设计文档

## 概述

允许服务器保持 `online-mode=true`（正版验证），同时自动放行离线（未验证）玩家。
在线玩家获得正版皮肤，离线玩家正常进服，无需配置白名单。

## 原理

基于服务端 `ServerLoginPacketListenerImpl` 的认证流程：

1. 客户端（正版/离线）发送 `ServerboundHelloPacket`
2. 服务端启动异步认证线程，调用 `MinecraftSessionService.hasJoinedServer()`
3. **在线玩家** → API 返回 `ProfileResult(realProfile)` → `startClientVerification(realProfile)` → 正常进服
4. **离线玩家** → API 返回 `null` → 调用 `disconnect()` → **Mixin 拦截** → 改为创建离线 `GameProfile` 并放行

## Mixin 方案

**目标类**: `net.minecraft.server.network.ServerLoginPacketListenerImpl`

**注入点**: `disconnect(Component)` 方法的开头

**逻辑**:

```java
@Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
private void onDisconnect(Component reason, CallbackInfo ci) {
    if (this.state == State.AUTHENTICATING) {
        // 离线玩家认证失败，放行
        GameProfile offline = UUIDUtil.createOfflineProfile(requestedUsername);
        startClientVerification(offline);
        LOGGER.info("{} logged in as offline player", requestedUsername);
        ci.cancel();
    }
}
```

### 为什么不在 `handleKey` 或匿名线程注入？

- 匿名内部类 (`ServerLoginPacketListenerImpl$1`) 无法被 Mixin 定位
- `disconnect` 是类上的实例方法，注入安全可靠
- `state == AUTHENTICATING` 条件确保只拦截认证阶段的失败断开

## 影响范围

| 场景 | 结果 |
|---|---|
| 在线玩家连接 | 认证通过，不影响 |
| 离线玩家连接 | 自动放行，使用离线 UUID |
| 认证服务器宕机 | 同离线放行（catch 块也调 disconnect） |
| 被踢出/被封禁 | 其他状态下的 disconnect 不受影响 |
| 正版玩家冒用 | 理论上可冒用名，但亲友服无风险 |

## 模组结构

```
onlinemodefix/
├── build.gradle
└── src/main/
    ├── java/com/ysh/onlinemodefix/
    │   ├── OnlineModeFixMod.java          # ModInitializer 入口
    │   └── mixin/
    │       └── ServerLoginMixin.java      # 核心 Mixin
    └── resources/
        ├── fabric.mod.json
        └── onlinemodefix.mixins.json
```

- 纯服务端模组 (`"environment": "server"`)
- 直接编译，无需修改 `server.properties`
