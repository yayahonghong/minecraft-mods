import json
from pathlib import Path
from mc_backup.manifest import (
    Manifest, FileInfo, load_manifest, save_manifest, diff_manifest,
)


def test_save_and_load(tmp_path: Path):
    m = Manifest(backup_time="2026-01-01T00:00:00")
    m.files["world/level.dat"] = FileInfo(size=100, mtime=1700000000)
    path = tmp_path / "manifest.json"
    save_manifest(m, path)
    loaded = load_manifest(path)
    assert loaded.backup_time == "2026-01-01T00:00:00"
    assert loaded.files["world/level.dat"].size == 100
    assert loaded.files["world/level.dat"].mtime == 1700000000


def test_load_manifest_not_found(tmp_path: Path):
    m = load_manifest(tmp_path / "nonexistent.json")
    assert m.backup_time == ""
    assert m.files == {}


def test_diff_manifest_new_file():
    old = Manifest()
    new = Manifest()
    new.files["a.dat"] = FileInfo(size=10, mtime=100)
    changes = diff_manifest(old, new)
    assert "a.dat" in changes


def test_diff_manifest_modified():
    old = Manifest()
    old.files["a.dat"] = FileInfo(size=10, mtime=100)
    new = Manifest()
    new.files["a.dat"] = FileInfo(size=20, mtime=200)
    changes = diff_manifest(old, new)
    assert "a.dat" in changes


def test_diff_manifest_unchanged():
    old = Manifest()
    old.files["a.dat"] = FileInfo(size=10, mtime=100)
    new = Manifest()
    new.files["a.dat"] = FileInfo(size=10, mtime=100)
    changes = diff_manifest(old, new)
    assert "a.dat" not in changes


def test_diff_manifest_deleted():
    old = Manifest()
    old.files["a.dat"] = FileInfo(size=10, mtime=100)
    new = Manifest()
    changes = diff_manifest(old, new)
    assert "a.dat" in changes
