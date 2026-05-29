from pathlib import Path
from unittest.mock import MagicMock, call, Mock
import pytest
from mc_backup.sftp_client import (
    RemoteFileInfo, scan_remote, download_files,
)


def test_scan_remote_collects_files():
    sftp_mock = MagicMock()
    sftp_mock.listdir_attr.side_effect = [
        [Mock(**{"filename": "a.dat", "st_mode": 0o100644, "st_size": 100, "st_mtime": 200})],
    ]
    results = scan_remote(sftp_mock, "/base", ["world"])
    assert "world/a.dat" in results
    assert results["world/a.dat"].size == 100
    assert results["world/a.dat"].mtime == 200


def test_scan_remote_nested_dirs():
    sftp_mock = MagicMock()
    sftp_mock.listdir_attr.side_effect = [
        [Mock(**{"filename": "sub", "st_mode": 0o40755, "st_size": 4096, "st_mtime": 100})],
        [Mock(**{"filename": "b.dat", "st_mode": 0o100644, "st_size": 50, "st_mtime": 300})],
    ]
    results = scan_remote(sftp_mock, "/base", ["world"])
    assert "world/sub/b.dat" in results
    assert results["world/sub/b.dat"].size == 50


def test_scan_remote_skips_nonexistent():
    sftp_mock = MagicMock()
    sftp_mock.listdir_attr.side_effect = FileNotFoundError
    results = scan_remote(sftp_mock, "/base", ["ghost"])
    assert results == {}


def test_download_files_downloads_changed(tmp_path: Path):
    sftp_mock = MagicMock()
    changed = {"world/a.dat", "world/sub/b.dat"}
    download_files(sftp_mock, "/base", changed, tmp_path)
    sftp_mock.get.assert_has_calls([
        call("/base/world/a.dat", str(tmp_path / "world" / "a.dat")),
        call("/base/world/sub/b.dat", str(tmp_path / "world" / "sub" / "b.dat")),
    ], any_order=True)
