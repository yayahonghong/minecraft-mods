import boto3
from pathlib import Path
from typing import Optional


def create_r2_client(cfg):
    endpoint = cfg.endpoint_url or f"https://{cfg.account_id}.r2.cloudflarestorage.com"
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


def copy_unchanged_files(
    client, bucket: str, snapshot: str,
    last_snapshot: Optional[str], unchanged: set[str],
) -> None:
    if not last_snapshot:
        return
    for rel_path in unchanged:
        src = _r2_key(last_snapshot, rel_path)
        dst = _r2_key(snapshot, rel_path)
        client.copy_object(
            Bucket=bucket, CopySource={"Bucket": bucket, "Key": src}, Key=dst,
        )


def _list_all_objects(client, bucket: str, prefix: str) -> list[dict]:
    keys: list[dict] = []
    paginator = client.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            keys.append({"Key": obj["Key"]})
    return keys


def _list_all_prefixes(client, bucket: str) -> list[str]:
    prefixes: list[str] = []
    paginator = client.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket, Prefix="backups/", Delimiter="/"):
        for p in page.get("CommonPrefixes", []):
            prefixes.append(p["Prefix"])
    return prefixes


def delete_old_snapshots(
    client, bucket: str, keep: int,
) -> None:
    if keep <= 0:
        return
    prefixes = _list_all_prefixes(client, bucket)
    prefixes.sort()
    to_delete = prefixes[:-keep] if len(prefixes) > keep else []
    for prefix in to_delete:
        keys = _list_all_objects(client, bucket, prefix)
        if keys:
            client.delete_objects(Bucket=bucket, Delete={"Objects": keys})
