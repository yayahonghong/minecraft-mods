# MC 存档备份至 Cloudflare R2 — 设计文档

## 概述

Python 脚本，通过 SFTP 从远程 Minecraft 服务器增量拉取存档，上传至 Cloudflare R2 存储，保留时间戳快照。

- 服务器仅开放 SFTP（无 Shell）
- 服务端类型：Fabric/Forge 模组服
- 备份策略：增量（基于文件 mtime/size 对比）

## 架构

```
mc-backup.py
├── Config (TOML)
├── SFTP Connector (paramiko)
├── Manifest Manager (JSON)
├── Incremental Downloader
├── R2 Uploader (boto3 S3 API)
└── Snapshot Cleaner (保留 N 份)
```

## R2 存储结构

```
backups/
├── 2026-05-29_143000/
│   ├── world/
│   │   ├── region/r.0.0.mca
│   │   ├── playerdata/*.dat
│   │   ├── level.dat
│   │   └── ...
│   └── world_nether/
│       └── ...
├── 2026-05-29_060000/
│   └── ...
└── ...
```

- 每次备份一个独立时间戳目录（格式 `YYYY-MM-DD_HHMMSS`）
- 未变化文件通过 R2 `CopyObject` API 从上次快照复制（内部免费）
- 仅变化/新增文件实际下载再上传

## 配置格式 (TOML)

```toml
[sftp]
host = "your-server.com"
port = 22
username = "mcuser"
private_key = "~/.ssh/id_ed25519"

[remote]
base_path = "/opt/minecraft/server"
worlds = ["world", "world_nether", "world_the_end"]
# 或简写：backup_dirs = ["world", "world_nether", "world_the_end"]

[r2]
account_id = "your-account-id"
bucket_name = "mc-backups"
access_key_id = "xxx"
secret_access_key = "xxx"
```

敏感字段可通过环境变量覆盖：
- `SFTP_PASSWORD`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`

## 核心流程

1. **读取配置** → 加载 TOML，合并环境变量覆盖
2. **SFTP 连接** → 用 paramiko 建立连接
3. **扫描远程目录** → 递归列出所有文件 + stat（mtime, size）
4. **比对 Manifest** → 找出新增/变化文件
5. **本地暂存** → 仅下载变化文件到本地临时目录
6. **R2 上传** → 将变化文件上传至 `backups/<timestamp>/`；未变文件从上次快照 CopyObject
7. **更新 Manifest** → 保存本次文件清单
8. **清除旧快照** → 删除超出 `--keep N` 的旧快照
9. **清理临时文件**

## Manifest 格式 (JSON)

```json
{
  "version": 1,
  "backup_time": "2026-05-29T14:30:00+08:00",
  "files": {
    "world/region/r.0.0.mca": {
      "size": 543210,
      "mtime": 1716964200
    },
    "world/level.dat": {
      "size": 2048,
      "mtime": 1716964200
    }
  }
}
```

## 命令行接口

```
usage: mc-backup.py [-h] [--config CONFIG] [--keep N] [--dry-run]

MC 存档备份至 Cloudflare R2

选项:
  --config CONFIG  配置文件路径 (默认: config.toml)
  --keep N         保留最近 N 份快照 (默认: 0 = 不清理)
  --dry-run        仅扫描，不执行上传
```

## 错误处理

- SFTP 连接失败 → 重试 3 次，指数退避
- 单文件下载失败 → 记录日志，继续其他文件
- R2 上传失败 → 重试 3 次，失败则标记快照不完整
- Manifest 损坏 → 从空清单重建执行全量备份

## Python 依赖

```
paramiko>=3.0
boto3>=1.28
tomli>=2.0       # Python 3.11+ 内置 tomllib，此条件备
```

## 非目标

- 不在备份前执行 Minecraft `save-all`（无 Shell 访问）
- 不提供 Web UI
- 不做跨平台 GUI
