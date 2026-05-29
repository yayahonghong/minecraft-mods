import argparse
import sys
from pathlib import Path
from mc_backup.config import load_config, ConfigError
from mc_backup.backup import run_backup


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="MC 存档备份至 Cloudflare R2",
    )
    parser.add_argument(
        "--config", type=str, default="config.toml",
        help="配置文件路径 (默认: config.toml)",
    )
    parser.add_argument(
        "--keep", type=int, default=0,
        help="保留最近 N 份快照 (默认: 0 = 不清理)",
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="仅扫描，不执行上传",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv or sys.argv[1:])
    cfg_path = Path(args.config)
    try:
        cfg = load_config(cfg_path)
    except ConfigError as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

    run_backup(cfg, keep=args.keep, dry_run=args.dry_run)
