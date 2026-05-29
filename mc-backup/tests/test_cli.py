from unittest.mock import patch
from mc_backup.cli import main


@patch("mc_backup.cli.load_config")
@patch("mc_backup.cli.run_backup")
def test_main_loads_default_config(mock_run, mock_load, tmp_path):
    with patch("mc_backup.cli.CONFIG_PATH", tmp_path / "config.toml"):
        (tmp_path / "config.toml").write_text("[backup]\nkeep = 3\n")
        main()
        mock_load.assert_called_once()
        mock_run.assert_called_once()
