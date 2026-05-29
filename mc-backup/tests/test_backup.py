from pathlib import Path
from unittest.mock import MagicMock, patch
from mc_backup.backup import run_backup
from mc_backup.config import Config, SFTPConfig, RemoteConfig, R2Config


def _make_config() -> Config:
    return Config(
        sftp=SFTPConfig(host="h", port=22, username="u", password="p"),
        remote=RemoteConfig(base_path="/mc", worlds=["world"]),
        r2=R2Config(account_id="a", bucket_name="b", access_key_id="k", secret_access_key="s"),
    )


@patch("mc_backup.backup.connect_sftp")
@patch("mc_backup.backup.scan_remote")
@patch("mc_backup.backup.create_r2_client")
@patch("mc_backup.backup.download_files")
def test_run_backup_full_flow(
    mock_dl, mock_r2, mock_scan, mock_connect, tmp_path,
):
    mock_sftp = MagicMock()
    mock_connect.return_value = mock_sftp
    mock_scan.return_value = {"world/level.dat": MagicMock(size=100, mtime=100.0)}
    mock_client = MagicMock()
    mock_r2.return_value = mock_client
    cfg = _make_config()

    run_backup(cfg, manifest_dir=tmp_path, staging_dir=str(tmp_path / "stage"))
    mock_connect.assert_called_once()
    mock_scan.assert_called_once()
    mock_dl.assert_called_once()
    mock_client.upload_file.assert_called_once()


@patch("mc_backup.backup.connect_sftp")
@patch("mc_backup.backup.scan_remote")
def test_run_backup_no_changes(mock_scan, mock_connect, tmp_path):
    mock_sftp = MagicMock()
    mock_connect.return_value = mock_sftp
    mock_scan.return_value = {}
    cfg = _make_config()

    run_backup(cfg, manifest_dir=tmp_path, staging_dir=str(tmp_path / "stage"))
    run_backup(cfg, manifest_dir=tmp_path, staging_dir=str(tmp_path / "stage"))

    assert mock_connect.call_count == 2
