#!/usr/bin/env python3
"""从 R2 backups/latest/ 恢复 Minecraft 存档到服务器"""

import logging
import sys
from pathlib import Path

from mc_backup.config import load_config, ConfigError
from mc_backup.r2_uploader import create_r2_client
from mc_backup.sftp_client import connect_sftp

SNAPSHOT = "latest"
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


def download_snapshot(client, bucket: str, dest: Path) -> None:
    paginator = client.get_paginator("list_objects_v2")
    prefix = f"backups/{SNAPSHOT}/"
    count = 0
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            rel_path = obj["Key"].removeprefix(prefix)
            local = dest / rel_path
            local.parent.mkdir(parents=True, exist_ok=True)
            client.download_file(bucket, obj["Key"], str(local))
            count += 1
    logger.info("Downloaded %d files to %s", count, dest)


def upload_to_server(sftp, base_path: str, local_dir: Path, worlds: list[str]) -> None:
    import posixpath
    for world in worlds:
        src = local_dir / world
        if not src.exists():
            logger.warning("World %s not found in snapshot, skipping", world)
            continue
        dst = posixpath.join(base_path, world)
        logger.info("Uploading %s -> %s", src, dst)
        _upload_dir(sftp, src, dst)


def _upload_dir(sftp, local: Path, remote: str) -> None:
    import posixpath
    try:
        sftp.stat(remote)
    except FileNotFoundError:
        sftp.mkdir(remote)
    for entry in local.iterdir():
        rel = entry.name
        if entry.is_dir():
            _upload_dir(sftp, entry, posixpath.join(remote, rel))
        else:
            sftp.put(str(entry), posixpath.join(remote, rel))


def main() -> None:
    confirm = input(f"从 R2 backups/{SNAPSHOT}/ 恢复到服务器？此操作将覆盖服务器上的 world 文件 [y/N] ")
    if confirm.lower() != "y":
        print("已取消")
        return

    try:
        cfg = load_config(Path("config.toml"))
    except ConfigError as e:
        logger.error("Config error: %s", e)
        sys.exit(1)

    r2 = create_r2_client(cfg.r2)
    dest = Path(f"restore_{SNAPSHOT}")
    dest.mkdir(parents=True, exist_ok=True)
    download_snapshot(r2, cfg.r2.bucket_name, dest)

    sftp = connect_sftp(cfg.sftp)
    try:
        upload_to_server(sftp, cfg.remote.base_path, dest, cfg.remote.worlds)
    finally:
        sftp.close()

    logger.info("恢复完成！本地临时文件保留在 %s，确认无误后可删除。", dest)


if __name__ == "__main__":
    main()
