# Minecraft Mods

多模组 Fabric 项目父工程。

## 子模组

| 模组 | 目录 | 说明 |
|---|---|---|
| ServerHelper | `serverhelper/` | 服务器管理模组，集成 QQ 通知与远程命令 |
| NoCreeperGrief | `nocreepergrief/` | 阻止苦力怕爆炸破坏地形，视觉改为真实烟花火箭效果 |
| ServerBot | `serverbot/` | 服务器假人模组，生成 Bot 挂机不掉线 |

## 构建

```bash
./gradlew build
```

各模组 JAR 输出在对应子目录的 `build/libs/` 下。

也可单独构建子模组：
```bash
./gradlew --no-daemon --console=plain :子模块名:build
```

## 环境要求

- Minecraft 26.1, Fabric Loader >= 0.18.5, Java >= 25
