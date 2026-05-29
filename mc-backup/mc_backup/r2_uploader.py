import boto3
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Optional

logger = __import__("logging").getLogger(__name__)


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
    max_workers: int = 5,
) -> None:
    if not changed:
        return
    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        futs = {}
        for rel_path in changed:
            key = _r2_key(snapshot, rel_path)
            local = staging_dir / rel_path
            futs[pool.submit(client.upload_file, str(local), bucket, key)] = rel_path
        for fut in as_completed(futs):
            fut.result()


def copy_unchanged_files(
    client, bucket: str, snapshot: str,
    last_snapshot: Optional[str], unchanged: set[str],
    max_workers: int = 10,
) -> None:
    if not last_snapshot or not unchanged:
        return
    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        futs = {}
        for rel_path in unchanged:
            src = _r2_key(last_snapshot, rel_path)
            dst = _r2_key(snapshot, rel_path)
            futs[pool.submit(
                client.copy_object,
                Bucket=bucket,
                CopySource={"Bucket": bucket, "Key": src},
                Key=dst,
            )] = rel_path
        for fut in as_completed(futs):
            fut.result()


def delete_files(
    client, bucket: str, snapshot: str,
    rel_paths: set[str],
) -> None:
    if not rel_paths:
        return
    keys = [{"Key": _r2_key(snapshot, p)} for p in rel_paths]
    client.delete_objects(Bucket=bucket, Delete={"Objects": keys})


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


def check_quota(client, bucket: str, max_mb: int) -> bool:
    """检查 R2 存储是否超过阈值，True=安全，False=超出"""
    if max_mb <= 0:
        return True
    total = 0
    paginator = client.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket):
        for obj in page.get("Contents", []):
            total += obj["Size"]
    used_mb = total / (1024 * 1024)
    if used_mb > max_mb:
        logger.warning("R2 已用 %.0f MB，超过阈值 %d MB，跳过备份以防止扣费",
                       used_mb, max_mb)
        return False
    logger.info("R2 已用 %.0f MB / 阈值 %d MB", used_mb, max_mb)
    return True


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
