# MC 存档备份至 R2 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过 SFTP 增量拉取 MC 存档并上传至 Cloudflare R2 存储

**Architecture:** 模块化 Python 包，paramiko 处理 SFTP，boto3 处理 R2 S3 API；每次备份创建时间戳快照目录，未变化文件通过 R2 CopyObject 内部拷贝

**Tech Stack:** Python 3.11+, paramiko, boto3, pytest

---

### Task 1: 项目脚手架与 Config 模块

**Files:**
- Create: `mc-backup/pyproject.toml`
- Create: `mc-backup/mc_backup/__init__.py`
- Create: `mc-backup/mc_backup/config.py`
- Create: `mc-backup/tests/test_config.py`
- Create: `mc-backup/tests/__init__.py`
- Create: `mc-backup/config.example.toml`

- [ ] **Step 1: 创建目录结构**

Run:
```bash
mkdir -p mc-backup/mc_backup mc-backup/tests
```

- [ ] **Step 2: 创建 pyproject.toml**

```toml
[project]
name = "mc-backup-r2"
version = "0.1.0"
description = "MC world backup via SFTP to Cloudflare R2"
requires-python = ">=3.11"
dependencies = [
    "paramiko>=3.0",
    "boto3>=1.28",
]

[project.scripts]
mc-backup = "mc_backup.cli:main"

[tool.pytest.ini_options]
testpaths = ["tests"]
```

- [ ] **Step 3: 创建 config.example.toml**

```toml
[sftp]
host = "your-server.com"
port = 22
username = "mcuser"
private_key = "~/.ssh/id_ed25519"

[remote]
base_path = "/opt/minecraft/server"
worlds = ["world", "world_nether", "world_the_end"]

[r2]
account_id = "your-account-id"
bucket_name = "mc-backups"
access_key_id = "CHANGEME"
secret_access_key = "CHANGEME"
```

- [ ] **Step 4: 创建 `mc_backup/__init__.py`**（空文件）

- [ ] **Step 5: 创建 `tests/__init__.py`**（空文件）

- [ ] **Step 6: 写测试 — Config 加载**

```python
# tests/test_config.py
import tomllib
from pathlib import Path
from mc_backup.config import Config, load_config

TOML_CONTENT = """
[sftp]
host = "example.com"
port = 22
username = "testuser"
private_key = "~/.ssh/test_key"

[remote]
base_path = "/srv/mc"
worlds = ["world"]

[r2]
account_id = "acct1"
bucket_name = "my-bucket"
access_key_id = "key1"
secret_access_key = "secret1"
"""

def test_load_config_parses_toml(tmp_path: Path):
    cfg_path = tmp_path / "config.toml"
    cfg_path.write_text(TOML_CONTENT)
    cfg = load_config(cfg_path)
    assert cfg.sftp.host == "example.com"
    assert cfg.sftp.port == 22
    assert cfg.remote.base_path == "/srv/mc"
    assert cfg.remote.worlds == ["world"]
    assert cfg.r2.bucket_name == "my-bucket"

def test_load_config_env_overrides(monkeypatch, tmp_path: Path):
    monkeypatch.setenv("R2_ACCESS_KEY_ID", "env_key")
    monkeypatch.setenv("R2_SECRET_ACCESS_KEY", "env_secret")
    cfg_path = tmp_path / "config.toml"
    cfg_path.write_text(TOML_CONTENT)
    cfg = load_config(cfg_path)
    assert cfg.r2.access_key_id == "env_key"
    assert cfg.r2.secret_access_key == "env_secret"

def test_load_config_not_found():
    from mc_backup.config import ConfigError
    import pytest
    with pytest.raises(ConfigError):
        load_config(Path("/nonexistent/config.toml"))
```

- [ ] **Step 7: 实现 Config 模块**

```python
# mc_backup/config.py
import os
import tomllib
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


class ConfigError(Exception):
    pass


@dataclass
class SFTPConfig:
    host: str
    port: int = 22
    username: str = ""
    password: Optional[str] = None
    private_key: Optional[str] = None


@dataclass
class RemoteConfig:
    base_path: str = ""
    worlds: list[str] = field(default_factory=list)


@dataclass
class R2Config:
    account_id: str = ""
    bucket_name: str = ""
    access_key_id: str = ""
    secret_access_key: str = ""
    endpoint_url: Optional[str] = None


@dataclass
class Config:
    sftp: SFTPConfig = field(default_factory=SFTPConfig)
    remote: RemoteConfig = field(default_factory=RemoteConfig)
    r2: R2Config = field(default_factory=R2Config)


def _env_overrides(cfg: Config) -> None:
    env_map = {
        "SFTP_PASSWORD": ("sftp", "password"),
        "R2_ACCESS_KEY_ID": ("r2", "access_key_id"),
        "R2_SECRET_ACCESS_KEY": ("r2", "secret_access_key"),
    }
    for env_key, (section, attr) in env_map.items():
        val = os.environ.get(env_key)
        if val is not None:
            getattr(cfg, section).__setattr__(attr, val)


def load_config(path: Path) -> Config:
    if not path.exists():
        raise ConfigError(f"Config file not found: {path}")
    raw = path.read_bytes()
    data = tomllib.loads(raw.decode("utf-8"))
    cfg = Config(
        sftp=SFTPConfig(**data.get("sftp", {})),
        remote=RemoteConfig(**data.get("remote", {})),
        r2=R2Config(**data.get("r2", {})),
    )
    _env_overrides(cfg)
    return cfg
```

- [ ] **Step 8: 运行测试验证通过**

```bash
cd mc-backup && python -m pytest tests/test_config.py -v
```

预期：3 个 test 全部 PASS

- [ ] **Step 9: 提交**

```bash
git add mc-backup/
git commit -m "feat(mc-backup): 项目脚手架和 config 模块"
```

---

### Task 2: Manifest 模块

**Files:**
- Create: `mc-backup/mc_backup/manifest.py`
- Create: `mc-backup/tests/test_manifest.py`

- [ ] **Step 1: 写测试**

```python
# tests/test_manifest.py
import json
from pathlib import Path
from mc_backup.manifest import (
    Manifest, FileInfo, load_manifest, save_manifest, diff_manifest,
)

def test_save_and_load(tmp_path: Path):
    m = Manifest(backup_time="2026-01-01T00:00:00")
    m.files["world/level.dat"] = FileInfo(size=100, mtime=1700000000)
    path = tmp_path / "manifest.json"
    save_manifest(m, path)
    loaded = load_manifest(path)
    assert loaded.backup_time == "2026-01-01T00:00:00"
    assert loaded.files["world/level.dat"].size == 100
    assert loaded.files["world/level.dat"].mtime == 1700000000

def test_load_manifest_not_found(tmp_path: Path):
    m = load_manifest(tmp_path / "nonexistent.json")
    assert m.backup_time == ""
    assert m.files == {}

def test_diff_manifest_new_file():
    old = Manifest()
    new = Manifest()
    new.files["a.dat"] = FileInfo(size=10, mtime=100)
    changes = diff_manifest(old, new)
    assert "a.dat" in changes  # new file → changed

def test_diff_manifest_modified():
    old = Manifest()
    old.files["a.dat"] = FileInfo(size=10, mtime=100)
    new = Manifest()
    new.files["a.dat"] = FileInfo(size=20, mtime=200)
    changes = diff_manifest(old, new)
    assert "a.dat" in changes

def test_diff_manifest_unchanged():
    old = Manifest()
    old.files["a.dat"] = FileInfo(size=10, mtime=100)
    new = Manifest()
    new.files["a.dat"] = FileInfo(size=10, mtime=100)
    changes = diff_manifest(old, new)
    assert "a.dat" not in changes
```

- [ ] **Step 2: 实现 Manifest 模块**

```python
# mc_backup/manifest.py
import json
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Optional


@dataclass
class FileInfo:
    size: int = 0
    mtime: float = 0.0


@dataclass
class Manifest:
    backup_time: str = ""
    files: dict[str, FileInfo] = field(default_factory=dict)


def load_manifest(path: Path) -> Manifest:
    if not path.exists():
        return Manifest()
    raw = path.read_text("utf-8")
    data = json.loads(raw)
    files = {
        k: FileInfo(**v) for k, v in data.get("files", {}).items()
    }
    return Manifest(backup_time=data.get("backup_time", ""), files=files)


def save_manifest(manifest: Manifest, path: Path) -> None:
    data = {
        "backup_time": manifest.backup_time,
        "files": {k: asdict(v) for k, v in manifest.files.items()},
    }
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), "utf-8")


def diff_manifest(old: Manifest, new: Manifest) -> set[str]:
    changed: set[str] = set()
    for path, info in new.files.items():
        old_info = old.files.get(path)
        if old_info is None or old_info.size != info.size or old_info.mtime != info.mtime:
            changed.add(path)
    return changed
```

- [ ] **Step 3: 运行测试验证通过**

```bash
cd mc-backup && python -m pytest tests/test_manifest.py -v
```

预期：5 个 test 全部 PASS

- [ ] **Step 4: 提交**

```bash
git add mc-backup/mc_backup/manifest.py mc-backup/tests/test_manifest.py
git commit -m "feat(mc-backup): manifest 模块 — 清单加载/比对"
```

---

### Task 3: SFTP 客户端模块

**Files:**
- Create: `mc-backup/mc_backup/sftp_client.py`
- Create: `mc-backup/tests/test_sftp_client.py`
- Modify: `mc-backup/tests/conftest.py`（共享 fixture）

- [ ] **Step 1: 创建 conftest.py（共享 fixture）**

```python
# tests/conftest.py
from pathlib import Path
from typing import Generator
import pytest
from unittest.mock import Mock, MagicMock, patch


@pytest.fixture
def mock_sftp_client() -> Generator[MagicMock, None, None]:
    with patch("paramiko.SFTPClient.from_transport") as mock:
        yield mock
```

- [ ] **Step 2: 写测试**

```python
# tests/test_sftp_client.py
from pathlib import Path
from unittest.mock import MagicMock, Mock, call
import pytest
from mc_backup.sftp_client import (
    RemoteFileInfo, scan_remote, download_files,
)


def test_scan_remote_collects_files():
    """scan_remote 递归列出远程文件并返回 dict[path, RemoteFileInfo]"""
    sftp_mock = MagicMock()
    # Simulate directory tree: dir/ a.dat, dir/sub/ b.dat
    sftp_mock.listdir_attr.side_effect = [
        [Mock(filename="dir", filename="dir", st_mode=0o40755, st_size=4096, st_mtime=100)],
        [Mock(filename="a.dat", filename="a.dat", st_mode=0o100644, st_size=100, st_mtime=200),
         Mock(filename="sub", filename="sub", st_mode=0o40755, st_size=4096, st_mtime=150)],
        [Mock(filename="b.dat", filename="b.dat", st_mode=0o100644, st_size=50, st_mtime=300)],
    ]
    results = scan_remote(sftp_mock, "/base", ["world"])
    assert "world/a.dat" in results
    assert results["world/a.dat"].size == 100
    assert results["world/a.dat"].mtime == 200
    assert "world/sub/b.dat" in results


def test_download_files_downloads_changed():
    sftp_mock = MagicMock()
    changed = {"world/a.dat", "world/sub/b.dat"}
    download_files(sftp_mock, "/base", changed, Path("/tmp/staging"))
    sftp_mock.get.assert_has_calls([
        call("/base/world/a.dat", str(Path("/tmp/staging/world/a.dat"))),
        call("/base/world/sub/b.dat", str(Path("/tmp/staging/world/sub/b.dat"))),
    ], any_order=True)
```

- [ ] **Step 3: 实现 SFTP 客户端模块**

```python
# mc_backup/sftp_client.py
import os
import stat
from dataclasses import dataclass
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

import paramiko


@dataclass
class RemoteFileInfo:
    size: int
    mtime: float


def connect_sftp(cfg) -> paramiko.SFTPClient:
    transport = paramiko.Transport((cfg.host, cfg.port))
    if cfg.private_key:
        key_path = os.path.expanduser(cfg.private_key)
        key = paramiko.Ed25519Key.from_private_key_file(key_path)
        transport.connect(username=cfg.username, pkey=key)
    elif cfg.password:
        transport.connect(username=cfg.username, password=cfg.password)
    else:
        transport.connect(username=cfg.username)
    return paramiko.SFTPClient.from_transport(transport)


def _scan_dir(sftp: paramiko.SFTPClient, base: str, prefix: str) -> dict[str, RemoteFileInfo]:
    results = {}
    full_path = os.path.join(base, prefix) if prefix else base
    try:
        entries = sftp.listdir_attr(full_path)
    except FileNotFoundError:
        return results
    for attr in entries:
        rel_path = f"{prefix}/{attr.filename}" if prefix else attr.filename
        if attr.st_mode is not None and stat.S_ISDIR(attr.st_mode):
            results.update(_scan_dir(sftp, base, rel_path))
        else:
            results[rel_path] = RemoteFileInfo(size=attr.st_size, mtime=float(attr.st_mtime))
    return results


def scan_remote(sftp, base_path: str, world_dirs: list[str]) -> dict[str, RemoteFileInfo]:
    files: dict[str, RemoteFileInfo] = {}
    for w in world_dirs:
        files.update(_scan_dir(sftp, base_path, w))
    return files


def download_files(sftp, base_path: str, changed: set[str], staging_dir: Path, max_workers: int = 4) -> None:
    def _download_one(rel_path: str) -> None:
        remote = os.path.join(base_path, rel_path)
        local = staging_dir / rel_path
        local.parent.mkdir(parents=True, exist_ok=True)
        sftp.get(remote, str(local))

    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        futs = {pool.submit(_download_one, p): p for p in changed}
        for fut in as_completed(futs):
            fut.result()
```

- [ ] **Step 4: 运行测试**

```bash
cd mc-backup && python -m pytest tests/test_sftp_client.py -v
```

预期：2 个 test 全部 PASS

- [ ] **Step 5: 提交**

```bash
git add mc-backup/mc_backup/sftp_client.py mc-backup/tests/test_sftp_client.py mc-backup/tests/conftest.py
git commit -m "feat(mc-backup): SFTP 客户端 — 远程扫描和增量下载"
```

---

### Task 4: R2 上传模块

**Files:**
- Create: `mc-backup/mc_backup/r2_uploader.py`
- Create: `mc-backup/tests/test_r2_uploader.py`

- [ ] **Step 1: 写测试**

```python
# tests/test_r2_uploader.py
from pathlib import Path
from unittest.mock import MagicMock, patch, call
from mc_backup.r2_uploader import (
    create_r2_client, upload_changed_files, delete_old_snapshots,
)


def test_create_r2_client():
    from mc_backup.config import R2Config
    cfg = R2Config(
        account_id="acct1",
        bucket_name="my-bucket",
        access_key_id="key1",
        secret_access_key="sec1",
    )
    with patch("boto3.client") as mock_boto:
        client = create_r2_client(cfg)
        mock_boto.assert_called_once()
        kwargs = mock_boto.call_args[1]
        assert kwargs["endpoint_url"] == "https://acct1.r2.cloudflarestorage.com"


def test_upload_changed_files(tmp_path: Path):
    client = MagicMock()
    staging = tmp_path / "stage"
    (staging / "world").mkdir(parents=True)
    (staging / "world" / "a.dat").write_bytes(b"data")
    changed = {"world/a.dat"}

    upload_changed_files(client, "my-bucket", "2026-05-29_143000", staging, changed)

    client.upload_file.assert_called_once_with(
        str(staging / "world" / "a.dat"),
        "my-bucket",
        "backups/2026-05-29_143000/world/a.dat",
    )


def test_delete_old_snapshots():
    client = MagicMock()
    client.list_objects_v2.return_value = {
        "CommonPrefixes": [
            {"Prefix": "backups/2026-05-28_120000/"},
            {"Prefix": "backups/2026-05-29_060000/"},
            {"Prefix": "backups/2026-05-29_143000/"},
        ]
    }

    delete_old_snapshots(client, "my-bucket", keep=2)

    # Should delete the oldest one (2026-05-28_120000)
    client.delete_objects.assert_called_once()
```

- [ ] **Step 2: 实现 R2 上传模块**

```python
# mc_backup/r2_uploader.py
import boto3
from pathlib import Path
from typing import Optional


def create_r2_client(cfg):
    endpoint = f"https://{cfg.account_id}.r2.cloudflarestorage.com"
    return boto3.client(
        "s3",
        endpoint_url=endpoint,
        aws_access_key_id=cfg.access_key_id,
        aws_secret_access_key=cfg.secret_access_key,
    )


def _r2_key(snapshot: str, rel_path: str) -> str:
    return f"backups/{snapshot}/{rel_path}"


def upload_changed_files(
    client, bucket: str, snapshot: str,
    staging_dir: Path, changed: set[str],
) -> None:
    for rel_path in changed:
        key = _r2_key(snapshot, rel_path)
        local = staging_dir / rel_path
        client.upload_file(str(local), bucket, key)


def copy_unchanged_files(
    client, bucket: str, snapshot: str,
    last_snapshot: Optional[str], unchanged: set[str],
) -> None:
    if not last_snapshot:
        return
    for rel_path in unchanged:
        src = f"backups/{last_snapshot}/{rel_path}"
        dst = _r2_key(snapshot, rel_path)
        client.copy_object(
            Bucket=bucket, CopySource={"Bucket": bucket, "Key": src}, Key=dst,
        )


def delete_old_snapshots(
    client, bucket: str, keep: int,
) -> None:
    resp = client.list_objects_v2(
        Bucket=bucket, Prefix="backups/", Delimiter="/",
    )
    prefixes = [p["Prefix"] for p in resp.get("CommonPrefixes", [])]
    prefixes.sort()
    to_delete = prefixes[:-keep] if keep > 0 and len(prefixes) > keep else []
    for prefix in to_delete:
        objs = client.list_objects_v2(Bucket=bucket, Prefix=prefix)
        keys = [{"Key": o["Key"]} for o in objs.get("Contents", [])]
        if keys:
            client.delete_objects(Bucket=bucket, Delete={"Objects": keys})
```

- [ ] **Step 3: 运行测试**

```bash
cd mc-backup && python -m pytest tests/test_r2_uploader.py -v
```

- [ ] **Step 4: 提交**

```bash
git add mc-backup/mc_backup/r2_uploader.py mc-backup/tests/test_r2_uploader.py
git commit -m "feat(mc-backup): R2 上传 — 增量快照上传和清理"
```

---

### Task 5: 主编排逻辑

**Files:**
- Create: `mc-backup/mc_backup/backup.py`
- Create: `mc-backup/tests/test_backup.py`
- Modify: `mc-backup/mc_backup/__init__.py`

- [ ] **Step 1: 写测试（集成场景）**

```python
# tests/test_backup.py
from pathlib import Path
from unittest.mock import MagicMock, patch
from mc_backup.backup import run_backup
from mc_backup.config import Config, SFTPConfig, RemoteConfig, R2Config


def _make_config(tmp_path: Path) -> Config:
    return Config(
        sftp=SFTPConfig(host="h", port=22, username="u", password="p"),
        remote=RemoteConfig(base_path="/mc", worlds=["world"]),
        r2=R2Config(account_id="a", bucket_name="b", access_key_id="k", secret_access_key="s"),
    )


@patch("mc_backup.backup.connect_sftp")
@patch("mc_backup.backup.scan_remote")
@patch("mc_backup.backup.create_r2_client")
def test_run_backup_full_flow(
    mock_r2, mock_scan, mock_connect, tmp_path,
):
    mock_sftp = MagicMock()
    mock_connect.return_value = mock_sftp
    mock_scan.return_value = {"world/level.dat": MagicMock(size=100, mtime=100)}
    mock_client = MagicMock()
    mock_r2.return_value = mock_client
    cfg = _make_config(tmp_path)

    run_backup(cfg, manifest_dir=tmp_path, staging_dir=str(tmp_path / "stage"))

    mock_connect.assert_called_once()
    mock_scan.assert_called_once()
    mock_client.upload_file.assert_called_once()
```

- [ ] **Step 2: 实现编排模块**

```python
# mc_backup/backup.py
import json
import os
import shutil
import tempfile
from datetime import datetime
from pathlib import Path
from typing import Optional

from mc_backup.config import Config
from mc_backup.manifest import (
    Manifest, FileInfo, load_manifest, save_manifest, diff_manifest,
)
from mc_backup.sftp_client import (
    connect_sftp, scan_remote, download_files, RemoteFileInfo,
)
from mc_backup.r2_uploader import (
    create_r2_client,
    upload_changed_files,
    copy_unchanged_files,
    delete_old_snapshots,
)


def run_backup(
    cfg: Config,
    manifest_dir: Optional[Path] = None,
    staging_dir: Optional[str] = None,
    keep: int = 0,
    dry_run: bool = False,
) -> None:
    if manifest_dir is None:
        manifest_dir = Path.cwd()
    manifest_path = manifest_dir / ".mc-backup-manifest.json"

    sftp = connect_sftp(cfg.sftp)
    try:
        remote_files = scan_remote(sftp, cfg.remote.base_path, cfg.remote.worlds)
    finally:
        sftp.close()

    # 构建新 manifest
    now_str = datetime.now().strftime("%Y-%m-%d_%H%M%S")
    new_manifest = Manifest(backup_time=now_str)
    for path, info in remote_files.items():
        new_manifest.files[path] = FileInfo(size=info.size, mtime=info.mtime)

    # 比对变化
    old_manifest = load_manifest(manifest_path)
    changed = diff_manifest(old_manifest, new_manifest)
    unchanged = set(new_manifest.files.keys()) - changed

    if not changed:
        print("No changes detected, skipping upload.")
        save_manifest(new_manifest, manifest_path)
        return

    # 下载变化文件
    stage = Path(staging_dir) if staging_dir else Path(tempfile.mkdtemp(prefix="mc-backup-"))
    try:
        download_files(sftp, cfg.remote.base_path, changed, stage)

        if dry_run:
            print(f"[DRY RUN] Would upload {len(changed)} changed files to backups/{now_str}/")
            return

        # 上传到 R2
        r2_client = create_r2_client(cfg.r2)
        upload_changed_files(r2_client, cfg.r2.bucket_name, now_str, stage, changed)

        # 从未变化文件从上个快照拷贝
        if old_manifest.backup_time:
            copy_unchanged_files(
                r2_client, cfg.r2.bucket_name, now_str,
                old_manifest.backup_time, unchanged,
            )

        # 清理旧快照
        if keep > 0:
            delete_old_snapshots(r2_client, cfg.r2.bucket_name, keep)
    finally:
        if not staging_dir:
            shutil.rmtree(stage, ignore_errors=True)

    save_manifest(new_manifest, manifest_path)
    print(f"Backup complete: backups/{now_str}/ ({len(changed)} files changed)")
```

- [ ] **Step 3: 更新 `__init__.py`**

```python
# mc_backup/__init__.py
from mc_backup.backup import run_backup
from mc_backup.config import Config, load_config

__all__ = ["run_backup", "Config", "load_config"]
```

- [ ] **Step 4: 运行测试**

```bash
cd mc-backup && python -m pytest tests/test_backup.py tests/test_config.py tests/test_manifest.py tests/test_sftp_client.py tests/test_r2_uploader.py -v
```

预期：全部 PASS

- [ ] **Step 5: 提交**

```bash
git add mc-backup/mc_backup/backup.py mc-backup/mc_backup/__init__.py mc-backup/tests/test_backup.py
git commit -m "feat(mc-backup): 主备份编排逻辑"
```

---

### Task 6: CLI 入口

**Files:**
- Create: `mc-backup/mc_backup/cli.py`
- Create: `mc-backup/tests/test_cli.py`

- [ ] **Step 1: 写测试**

```python
# tests/test_cli.py
from pathlib import Path
from unittest.mock import patch
from mc_backup.cli import parse_args, main


def test_parse_args_defaults():
    args = parse_args(["--config", "my.toml"])
    assert args.config == "my.toml"
    assert args.keep == 0
    assert args.dry_run is False


def test_parse_args_all():
    args = parse_args(["--config", "c.toml", "--keep", "7", "--dry-run"])
    assert args.config == "c.toml"
    assert args.keep == 7
    assert args.dry_run is True


@patch("mc_backup.cli.load_config")
@patch("mc_backup.cli.run_backup")
def test_main_integration(mock_run, mock_load, tmp_path):
    cfg_path = tmp_path / "config.toml"
    cfg_path.write_text("")
    mock_load.return_value = "fake_cfg"

    main([str(cfg_path)])

    mock_load.assert_called_once()
    mock_run.assert_called_once()
```

- [ ] **Step 2: 实现 CLI 模块**

```python
# mc_backup/cli.py
import argparse
import sys
from pathlib import Path
from mc_backup.config import load_config, ConfigError
from mc_backup.backup import run_backup


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="MC 存档备份至 Cloudflare R2",
    )
    parser.add_argument(
        "--config", type=str, default="config.toml",
        help="配置文件路径 (默认: config.toml)",
    )
    parser.add_argument(
        "--keep", type=int, default=0,
        help="保留最近 N 份快照 (默认: 0 = 不清理)",
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="仅扫描，不执行上传",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv or sys.argv[1:])
    cfg_path = Path(args.config)
    try:
        cfg = load_config(cfg_path)
    except ConfigError as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

    run_backup(cfg, keep=args.keep, dry_run=args.dry_run)
```

- [ ] **Step 3: 运行测试**

```bash
cd mc-backup && python -m pytest tests/test_cli.py -v
```

- [ ] **Step 4: 最终运行全部测试**

```bash
cd mc-backup && python -m pytest tests/ -v
```

- [ ] **Step 5: 提交**

```bash
git add mc-backup/mc_backup/cli.py mc-backup/tests/test_cli.py
git commit -m "feat(mc-backup): CLI 入口和 argparse"
```

---

### Task 7: 集成验证 & 文档

**Files:**
- Modify: `mc-backup/pyproject.toml`（如果需要补充）
- Modify: `.gitignore`（添加 config.toml）

- [ ] **Step 1: 更新 .gitignore**

在项目根目录 `.gitignore` 中添加：
```
# mc-backup
mc-backup/config.toml
```

- [ ] **Step 2: 验证安装**

```bash
cd mc-backup && pip install -e .
```

- [ ] **Step 3: 验证 CLI 可用**

```bash
mc-backup --help
```

预期：打印帮助信息

- [ ] **Step 4: 提交**

```bash
git add mc-backup/pyproject.toml .gitignore
git commit -m "chore(mc-backup): 集成配置和 .gitignore"
```
