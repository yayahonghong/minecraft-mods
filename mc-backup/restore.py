#!/usr/bin/env python3
"""从 R2 快照恢复 Minecraft 存档到服务器"""

import logging
import sys
from pathlib import Path

from mc_backup.config import load_config, ConfigError
from mc_backup.r2_uploader import create_r2_client
from mc_backup.sftp_client import connect_sftp

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


def list_snapshots(client, bucket: str) -> list[str]:
    paginator = client.get_paginator("list_objects_v2")
    snapshots: list[str] = []
    for page in paginator.paginate(Bucket=bucket, Prefix="backups/", Delimiter="/"):
        for p in page.get("CommonPrefixes", []):
            snapshots.append(p["Prefix"].removeprefix("backups/").removesuffix("/"))
    return sorted(snapshots, reverse=True)


def download_snapshot(client, bucket: str, snapshot: str, dest: Path) -> None:
    paginator = client.get_paginator("list_objects_v2")
    prefix = f"backups/{snapshot}/"
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
    try:
        cfg = load_config(Path("config.toml"))
    except ConfigError as e:
        logger.error("Config error: %s", e)
        sys.exit(1)

    r2 = create_r2_client(cfg.r2)
    bucket = cfg.r2.bucket_name

    snapshots = list_snapshots(r2, bucket)
    if not snapshots:
        logger.error("No snapshots found in R2")
        sys.exit(1)

    print("\nAvailable snapshots:")
    for i, snap in enumerate(snapshots, 1):
        print(f"  {i}. {snap}")

    if len(sys.argv) > 1:
        choice = sys.argv[1]
        if choice in snapshots:
            snapshot = choice
        else:
            logger.error("Snapshot '%s' not found", choice)
            sys.exit(1)
    else:
        print(f"\n选择要恢复的快照 (1-{len(snapshots)})，或 Ctrl+C 取消:")
        try:
            idx = int(input("> ")) - 1
            snapshot = snapshots[idx]
        except (ValueError, IndexError):
            logger.error("Invalid selection")
            sys.exit(1)

    logger.info("Restoring snapshot: %s", snapshot)

    dest = Path(f"restore_{snapshot}")
    dest.mkdir(parents=True, exist_ok=True)

    download_snapshot(r2, bucket, snapshot, dest)

    sftp = connect_sftp(cfg.sftp)
    try:
        upload_to_server(sftp, cfg.remote.base_path, dest, cfg.remote.worlds)
    finally:
        sftp.close()

    logger.info("Restore complete! Snapshot files kept at %s for verification.", dest)


if __name__ == "__main__":
    main()
