import sys
from pathlib import Path
from mc_backup.config import load_config, ConfigError
from mc_backup.backup import run_backup

CONFIG_PATH = Path("config.toml")


def main(argv: list[str] | None = None) -> None:
    dry_run = "--dry-run" in (argv or sys.argv[1:])

    try:
        cfg = load_config(CONFIG_PATH)
    except ConfigError as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

    run_backup(cfg, dry_run=dry_run)
