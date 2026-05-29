import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

try:
    import tomllib
except ImportError:
    import tomli as tomllib  # Python < 3.11


class ConfigError(Exception):
    pass


@dataclass
class SFTPConfig:
    host: str = ""
    port: int = 22
    username: str = ""
    password: Optional[str] = None
    private_key: Optional[str] = None


@dataclass
class RemoteConfig:
    base_path: str = ""
    worlds: list[str] = field(default_factory=list)


@dataclass
class R2Config:
    account_id: str = ""
    bucket_name: str = ""
    access_key_id: str = ""
    secret_access_key: str = ""
    endpoint_url: Optional[str] = None


@dataclass
class BackupConfig:
    keep: int = 0


@dataclass
class Config:
    sftp: SFTPConfig = field(default_factory=SFTPConfig)
    remote: RemoteConfig = field(default_factory=RemoteConfig)
    r2: R2Config = field(default_factory=R2Config)
    backup: BackupConfig = field(default_factory=BackupConfig)


def _env_overrides(cfg: Config) -> None:
    env_map = {
        "SFTP_PASSWORD": ("sftp", "password"),
        "R2_ACCESS_KEY_ID": ("r2", "access_key_id"),
        "R2_SECRET_ACCESS_KEY": ("r2", "secret_access_key"),
    }
    for env_key, (section, attr) in env_map.items():
        val = os.environ.get(env_key)
        if val is not None:
            setattr(getattr(cfg, section), attr, val)


def load_config(path: Path | str) -> Config:
    if isinstance(path, str):
        path = Path(path)
    if not path.exists():
        raise ConfigError(f"Config file not found: {path}")
    data = tomllib.loads(path.read_text(encoding="utf-8"))
    cfg = Config(
        sftp=SFTPConfig(**data.get("sftp", {})),
        remote=RemoteConfig(**data.get("remote", {})),
        r2=R2Config(**data.get("r2", {})),
        backup=BackupConfig(**data.get("backup", {})),
    )
    _env_overrides(cfg)
    return cfg
