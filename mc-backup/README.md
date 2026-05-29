# mc-backup

通过 SFTP 从 Minecraft 服务端拉取存档，增量备份至 Cloudflare R2。

## 用法

```bash
# 拷贝配置并编辑
cp config.example.toml config.toml
# 编辑 config.toml，填入 SFTP / R2 凭据

# 运行备份
python3 run.py

# 模拟运行（不实际传输）
python3 run.py --dry-run
```

## 配置

参见 `config.example.toml`。

### SFTP

支持密码或密钥认证。`host` / `port` / `username` 为必需。

### Remote

- `base_path` — SFTP 上存档根目录（通常为空 `/` 或 `/opt/minecraft/server`）
- `worlds` — 需要备份的世界文件夹名列表

### R2

需要 Cloudflare R2 的 **Account ID**（R2 后台 Overview 页面）以及 **API 令牌**（R2 → 管理 R2 API 令牌 → 创建令牌，权限：编辑）。

### Backup

- `keep` — 保留最近 N 份快照，`0` 表示不自动清理

## 流程

1. 连接 SFTP，扫描 world 目录下所有文件
2. 与上次备份清单 (`.mc-backup-manifest.json`) 比对，找出新增/修改/删除的文件
3. 从 SFTP 下载新增/修改的文件
4. 上传到 R2 `backups/<时间戳>/` 目录
5. 未变更的文件**并行复制**从上一份快照（`copy_object`，10 线程）
6. 按 `keep` 策略清理过期快照
7. 保存新的备份清单

## 依赖

```
paramiko>=3.0
boto3>=1.28
tomli>=2.0   # Python < 3.11
```
