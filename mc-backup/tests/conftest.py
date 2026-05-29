from pathlib import Path
from typing import Generator
import pytest
from unittest.mock import MagicMock, patch


@pytest.fixture
def mock_sftp_client() -> Generator[MagicMock, None, None]:
    with patch("paramiko.SFTPClient.from_transport") as mock:
        yield mock
