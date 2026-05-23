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
