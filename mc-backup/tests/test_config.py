from pathlib import Path
from mc_backup.config import Config, load_config

TOML_CONTENT = """
[sftp]
host = "example.com"
port = 22
username = "testuser"
private_key = "~/.ssh/test_key"

[remote]
base_path = "/srv/mc"
worlds = ["world"]

[r2]
account_id = "acct1"
bucket_name = "my-bucket"
access_key_id = "key1"
secret_access_key = "secret1"

[backup]
keep = 7
max_storage_mb = 5000
"""

def test_load_config_parses_toml(tmp_path: Path):
    cfg_path = tmp_path / "config.toml"
    cfg_path.write_text(TOML_CONTENT)
    cfg = load_config(cfg_path)
    assert cfg.sftp.host == "example.com"
    assert cfg.sftp.port == 22
    assert cfg.remote.base_path == "/srv/mc"
    assert cfg.remote.worlds == ["world"]
    assert cfg.r2.bucket_name == "my-bucket"

def test_load_config_env_overrides(monkeypatch, tmp_path: Path):
    monkeypatch.setenv("R2_ACCESS_KEY_ID", "env_key")
    monkeypatch.setenv("R2_SECRET_ACCESS_KEY", "env_secret")
    cfg_path = tmp_path / "config.toml"
    cfg_path.write_text(TOML_CONTENT)
    cfg = load_config(cfg_path)
    assert cfg.r2.access_key_id == "env_key"
    assert cfg.r2.secret_access_key == "env_secret"

def test_load_config_backup_section(tmp_path: Path):
    cfg_path = tmp_path / "config.toml"
    cfg_path.write_text(TOML_CONTENT)
    cfg = load_config(cfg_path)
    assert cfg.backup.keep == 7
    assert cfg.backup.max_storage_mb == 5000

def test_load_config_not_found():
    from mc_backup.config import ConfigError
    import pytest
    with pytest.raises(ConfigError):
        load_config(Path("/nonexistent/config.toml"))
