from pathlib import Path
from unittest.mock import patch
from mc_backup.cli import parse_args, main


def test_parse_args_defaults():
    args = parse_args([])
    assert args.config == "config.toml"
    assert args.keep == 0
    assert args.dry_run is False


def test_parse_args_all():
    args = parse_args(["--config", "c.toml", "--keep", "7", "--dry-run"])
    assert args.config == "c.toml"
    assert args.keep == 7
    assert args.dry_run is True


@patch("mc_backup.cli.load_config")
@patch("mc_backup.cli.run_backup")
def test_main_integration(mock_run, mock_load):
    mock_load.return_value = "fake_cfg"
    main(["--config", "test.toml"])
    mock_load.assert_called_once()
    mock_run.assert_called_once()
