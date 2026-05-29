import logging
import shutil
import tempfile
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
    delete_files,
)

SNAPSHOT_NAME = "latest"


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

        new_manifest = Manifest(backup_time=SNAPSHOT_NAME)
        for path, info in remote_files.items():
            new_manifest.files[path] = FileInfo(size=info.size, mtime=info.mtime)

        old_manifest = load_manifest(manifest_path)
        all_changed = diff_manifest(old_manifest, new_manifest)
        new_keys = set(new_manifest.files.keys())
        deleted = set(old_manifest.files.keys()) - new_keys
        changed = all_changed - deleted

        logger.info("Changes: %d new/modified, %d deleted, %d unchanged",
                     len(changed), len(deleted), len(new_keys - all_changed))

        if not all_changed and not deleted:
            logger.info("No changes detected, nothing to do.")
            return

        stage = Path(staging_dir) if staging_dir else Path(tempfile.mkdtemp(prefix="mc-backup-"))
        try:
            if changed:
                logger.info("Downloading %d changed files from SFTP...", len(changed))
                download_files(sftp, cfg.remote.base_path, changed, stage)

            if dry_run:
                logger.info("[DRY RUN] Upload %d changed, delete %d from R2 backups/%s/",
                           len(changed), len(deleted), SNAPSHOT_NAME)
                save_manifest(new_manifest, manifest_path)
                return

            logger.info("Connecting to Cloudflare R2...")
            r2_client = create_r2_client(cfg.r2)

            if changed:
                logger.info("Uploading %d files to backups/%s/...", len(changed), SNAPSHOT_NAME)
                upload_changed_files(r2_client, cfg.r2.bucket_name, SNAPSHOT_NAME, stage, changed)

            if deleted:
                logger.info("Deleting %d files from R2 (no longer on server)...", len(deleted))
                delete_files(r2_client, cfg.r2.bucket_name, SNAPSHOT_NAME, deleted)
        finally:
            if not staging_dir:
                shutil.rmtree(stage, ignore_errors=True)

        save_manifest(new_manifest, manifest_path)
        logger.info("Backup complete: backups/%s/ (%d uploaded, %d deleted)",
                    SNAPSHOT_NAME, len(changed), len(deleted))
    except Exception:
        logger.exception("Backup failed")
        raise
    finally:
        sftp.close()
