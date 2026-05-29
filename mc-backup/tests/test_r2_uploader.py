from pathlib import Path
from unittest.mock import MagicMock, patch, call
from mc_backup.r2_uploader import (
    create_r2_client, upload_changed_files, copy_unchanged_files,
    delete_files, delete_old_snapshots,
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


def test_copy_unchanged_files():
    client = MagicMock()
    copy_unchanged_files(client, "my-bucket", "snap2", "snap1", {"world/a.dat"}, max_workers=1)
    client.copy_object.assert_called_once_with(
        Bucket="my-bucket",
        CopySource={"Bucket": "my-bucket", "Key": "backups/snap1/world/a.dat"},
        Key="backups/snap2/world/a.dat",
    )


def test_delete_old_snapshots():
    client = MagicMock()
    paginator = MagicMock()
    call_count = [0]

    def paginate_side_effect(**kwargs):
        idx = call_count[0]
        call_count[0] += 1
        if kwargs.get("Delimiter") == "/":
            return [{"CommonPrefixes": [{"Prefix": "backups/2026-05-28_120000/"}, {"Prefix": "backups/2026-05-29_060000/"}, {"Prefix": "backups/2026-05-29_143000/"}]}]
        return [{"Contents": [{"Key": "backups/2026-05-28_120000/world/a.dat"}]}]

    paginator.paginate.side_effect = paginate_side_effect
    client.get_paginator.return_value = paginator

    delete_old_snapshots(client, "my-bucket", keep=2)
    client.delete_objects.assert_called_once_with(
        Bucket="my-bucket",
        Delete={"Objects": [{"Key": "backups/2026-05-28_120000/world/a.dat"}]},
    )


def test_delete_files():
    client = MagicMock()
    delete_files(client, "my-bucket", "snap1", {"world/a.dat", "world/b.dat"})
    args, kwargs = client.delete_objects.call_args
    assert kwargs["Bucket"] == "my-bucket"
    keys = {o["Key"] for o in kwargs["Delete"]["Objects"]}
    assert keys == {"backups/snap1/world/a.dat", "backups/snap1/world/b.dat"}
