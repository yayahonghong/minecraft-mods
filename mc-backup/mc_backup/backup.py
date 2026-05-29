import logging
import shutil
import tempfile
from datetime import datetime
from pathlib import Path
from typing import Optional

from mc_backup.config import Config

logger = logging.getLogger(__name__)
from mc_backup.manifest import (
    Manifest, FileInfo, load_manifest, save_manifest, diff_manifest,
)
from mc_backup.sftp_client import (
    connect_sftp, scan_remote, download_files,
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
    dry_run: bool = False,
) -> None:
    if manifest_dir is None:
        manifest_dir = Path.cwd()
    manifest_path = manifest_dir / ".mc-backup-manifest.json"

    sftp = connect_sftp(cfg.sftp)
    try:
        logger.info("Connecting to SFTP and scanning remote files...")
        remote_files = scan_remote(sftp, cfg.remote.base_path, cfg.remote.worlds)
        logger.info("Found %d remote files", len(remote_files))

        now_str = datetime.now().strftime("%Y-%m-%d_%H%M%S")
        new_manifest = Manifest(backup_time=now_str)
        for path, info in remote_files.items():
            new_manifest.files[path] = FileInfo(size=info.size, mtime=info.mtime)

        old_manifest = load_manifest(manifest_path)
        all_changed = diff_manifest(old_manifest, new_manifest)
        new_keys = set(new_manifest.files.keys())
        unchanged = new_keys - all_changed
        deleted = set(old_manifest.files.keys()) - new_keys
        changed = all_changed - deleted

        logger.info("Changes: %d new/modified, %d deleted, %d unchanged", len(changed), len(deleted), len(unchanged))

        if not all_changed:
            logger.info("No changes detected, skipping upload.")
            save_manifest(new_manifest, manifest_path)
            return

        stage = Path(staging_dir) if staging_dir else Path(tempfile.mkdtemp(prefix="mc-backup-"))
        try:
            if changed:
                logger.info("Downloading %d changed files from SFTP...", len(changed))
                download_files(sftp, cfg.remote.base_path, changed, stage)
            if deleted:
                logger.warning("%d files were deleted on remote, skipping in backup", len(deleted))

            if dry_run:
                logger.info("[DRY RUN] Would upload %d new/modified + %d unchanged files to backups/%s/",
                           len(changed), len(unchanged), now_str)
                return

            logger.info("Connecting to Cloudflare R2...")
            r2_client = create_r2_client(cfg.r2)
            if changed:
                logger.info("Uploading %d changed files to backups/%s/...", len(changed), now_str)
                upload_changed_files(r2_client, cfg.r2.bucket_name, now_str, stage, changed)
            if unchanged and old_manifest.backup_time:
                logger.info("Copying %d unchanged files from previous snapshot...", len(unchanged))
                copy_unchanged_files(
                    r2_client, cfg.r2.bucket_name, now_str,
                    old_manifest.backup_time, unchanged,
                )

            if cfg.backup.keep > 0:
                logger.info("Cleaning old snapshots, keeping last %d...", cfg.backup.keep)
                delete_old_snapshots(r2_client, cfg.r2.bucket_name, cfg.backup.keep)
        finally:
            if not staging_dir:
                shutil.rmtree(stage, ignore_errors=True)

        save_manifest(new_manifest, manifest_path)
        logger.info("Backup complete: backups/%s/ (%d new/modified, %d deleted)", now_str, len(changed), len(deleted))
    except Exception:
        logger.exception("Backup failed")
        raise
    finally:
        sftp.close()
