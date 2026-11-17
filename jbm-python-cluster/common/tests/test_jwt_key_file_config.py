import pytest

from jbm_cluster_py.common.config import _env_overrides


def test_persistent_key_file_and_explicit_environment_precedence(monkeypatch, tmp_path):
    key = tmp_path / "signing.pem"
    key.write_text("persistent signing material\n")
    monkeypatch.delenv("JBM_CONFIG_JSON", raising=False)
    monkeypatch.delenv("JBM_AUTH_JWT_PRIVATE_KEY", raising=False)
    monkeypatch.setenv("JBM_AUTH_JWT_PRIVATE_KEY_FILE", str(key))
    assert _env_overrides()["jbm"]["auth"]["jwt"]["private-key"] == key.read_text()
    monkeypatch.setenv("JBM_AUTH_JWT_PRIVATE_KEY", "explicit override")
    assert _env_overrides()["jbm"]["auth"]["jwt"]["private-key"] == "explicit override"


@pytest.mark.parametrize("exists", [False, True])
def test_unavailable_or_empty_key_file_fails_closed(monkeypatch, tmp_path, exists):
    key = tmp_path / "signing.pem"
    if exists:
        key.write_text("\n")
    monkeypatch.delenv("JBM_AUTH_JWT_PRIVATE_KEY", raising=False)
    monkeypatch.setenv("JBM_AUTH_JWT_PRIVATE_KEY_FILE", str(key))
    with pytest.raises(ValueError if exists else FileNotFoundError):
        _env_overrides()
