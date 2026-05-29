import boto3
from pathlib import Path
from typing import Optional


def create_r2_client(cfg):
    if cfg.account_id:
        endpoint = f"https://{cfg.account_id}.r2.cloudflarestorage.com"
    elif cfg.endpoint_url:
        endpoint = cfg.endpoint_url
    else:
        raise ValueError("R2: account_id 或 endpoint_url 必须至少设置一个")
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


def delete_files(
    client, bucket: str, snapshot: str,
    rel_paths: set[str],
) -> None:
    if not rel_paths:
        return
    keys = [{"Key": _r2_key(snapshot, p)} for p in rel_paths]
    client.delete_objects(Bucket=bucket, Delete={"Objects": keys})


