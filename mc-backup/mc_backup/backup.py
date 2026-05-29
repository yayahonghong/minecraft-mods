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
    keep: int = 0,
    dry_run: bool = False,
) -> None:
    if manifest_dir is None:
        manifest_dir = Path.cwd()
    manifest_path = manifest_dir / ".mc-backup-manifest.json"

    sftp = connect_sftp(cfg.sftp)
    try:
        remote_files = scan_remote(sftp, cfg.remote.base_path, cfg.remote.worlds)

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

        if not all_changed:
            print("No changes detected, skipping upload.")
            save_manifest(new_manifest, manifest_path)
            return

        stage = Path(staging_dir) if staging_dir else Path(tempfile.mkdtemp(prefix="mc-backup-"))
        try:
            if changed:
                download_files(sftp, cfg.remote.base_path, changed, stage)
            if deleted:
                print(f"Skipping {len(deleted)} deleted files (absent from new snapshot)")

            if dry_run:
                print(f"[DRY RUN] Would upload {len(changed)} new/modified + {len(unchanged)} unchanged files to backups/{now_str}/")
                return

            r2_client = create_r2_client(cfg.r2)
            if changed:
                upload_changed_files(r2_client, cfg.r2.bucket_name, now_str, stage, changed)
            if unchanged and old_manifest.backup_time:
                copy_unchanged_files(
                    r2_client, cfg.r2.bucket_name, now_str,
                    old_manifest.backup_time, unchanged,
                )

            if keep > 0:
                delete_old_snapshots(r2_client, cfg.r2.bucket_name, keep)
        finally:
            if not staging_dir:
                shutil.rmtree(stage, ignore_errors=True)

        save_manifest(new_manifest, manifest_path)
        print(f"Backup complete: backups/{now_str}/ ({len(changed)} new/modified, {len(deleted)} deleted)")
    finally:
        sftp.close()
