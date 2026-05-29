import logging
import os
import posixpath
import stat
import threading
from dataclasses import dataclass
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

import paramiko

logger = logging.getLogger(__name__)


@dataclass
class RemoteFileInfo:
    size: int
    mtime: float


def connect_sftp(cfg) -> paramiko.SFTPClient:
    transport = paramiko.Transport((cfg.host, cfg.port))
    try:
        if cfg.private_key:
            key_path = os.path.expanduser(cfg.private_key)
            key = paramiko.Ed25519Key.from_private_key_file(key_path)
            transport.connect(username=cfg.username, pkey=key)
        elif cfg.password:
            transport.connect(username=cfg.username, password=cfg.password)
        else:
            transport.connect(username=cfg.username)
        return paramiko.SFTPClient.from_transport(transport)
    except:
        transport.close()
        raise


def _scan_dir(sftp, base: str, prefix: str) -> dict[str, RemoteFileInfo]:
    results = {}
    full_path = posixpath.join(base, prefix) if prefix else base
    try:
        entries = sftp.listdir_attr(full_path)
    except OSError as e:
        logger.warning("Cannot list directory %s: %s", full_path, e)
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
    _lock = threading.Lock()

    def _download_one(rel_path: str) -> None:
        remote = posixpath.join(base_path, rel_path)
        local = staging_dir / rel_path
        local.parent.mkdir(parents=True, exist_ok=True)
        with _lock:
            sftp.get(remote, str(local))

    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        futs = {pool.submit(_download_one, p): p for p in changed}
        for fut in as_completed(futs):
            fut.result()
