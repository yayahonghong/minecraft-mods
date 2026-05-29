import json
from dataclasses import dataclass, field, asdict
from pathlib import Path


@dataclass
class FileInfo:
    size: int = 0
    mtime: float = 0.0


@dataclass
class Manifest:
    backup_time: str = ""
    files: dict[str, FileInfo] = field(default_factory=dict)


def load_manifest(path: Path) -> Manifest:
    if not path.exists():
        return Manifest()
    raw = path.read_text("utf-8")
    data = json.loads(raw)
    files = {
        k: FileInfo(**v) for k, v in data.get("files", {}).items()
    }
    return Manifest(backup_time=data.get("backup_time", ""), files=files)


def save_manifest(manifest: Manifest, path: Path) -> None:
    data = {
        "backup_time": manifest.backup_time,
        "files": {k: asdict(v) for k, v in manifest.files.items()},
    }
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), "utf-8")


def diff_manifest(old: Manifest, new: Manifest) -> set[str]:
    changed: set[str] = set()
    for path, info in new.files.items():
        old_info = old.files.get(path)
        if old_info is None or old_info.size != info.size or old_info.mtime != info.mtime:
            changed.add(path)
    for path in old.files:
        if path not in new.files:
            changed.add(path)
    return changed
