from __future__ import annotations

from pathlib import Path

import pytest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi.testclient import TestClient
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.auth.main import _validate_production_auth_config, create_app
from jbm_cluster_py.platform.auth.service import AuthError


@pytest.mark.parametrize("profile", ["prod", "production", "staging"])
@pytest.mark.parametrize(
    "flag,value",
    [
        ("require-pkce", "false"),
        ("require-https-redirects", "false"),
        ("dev-bypass-enabled", "true"),
        ("allow-plaintext-secrets", "true"),
        ("legacy-password-grant-enabled", "true"),
        ("fixed-captcha-code", "9999"),
    ],
)
def test_production_configuration_handles_string_flags(profile, flag, value):
    config = {"jwt": {"issuer": "https://auth.example", "private-key": "provided"}, flag: value}
    with pytest.raises(RuntimeError, match=flag):
        _validate_production_auth_config(profile, config)


@pytest.mark.parametrize(
    "issuer",
    [
        "http://auth.example/oidc",
        "https://auth.example/other",
        "https://auth.example/oidc?x=1",
        "https://auth.example/oidc#x",
        "https://user:password@auth.example/oidc",
    ],
)
def test_production_oidc_issuer_must_match_mounted_endpoints(issuer):
    config = {
        "jwt": {"issuer": "https://auth.example", "private-key": "provided"},
        "oidc": {"enabled": True, "issuer": issuer},
    }
    with pytest.raises(RuntimeError, match="oidc.issuer"):
        _validate_production_auth_config("prod", config)


def test_production_does_not_require_oidc_when_disabled():
    _validate_production_auth_config(
        "prod",
        {
            "jwt": {"issuer": "https://auth.example", "private-key": "provided"},
            "oidc": {"enabled": "false"},
            "dev-bypass-enabled": "false",
        },
    )


def test_readiness_uses_live_dependency_state(tmp_path: Path, monkeypatch):
    config = AppConfig(
        {
            "server": {"port": 5555},
            "spring": {"application": {"name": "auth-test"}},
            "integrations": {
                "database": {"url": f"sqlite+aiosqlite:///{tmp_path / 'ready.db'}"},
                "redis": {"enabled": False},
            },
            "jbm": {"auth": {}},
        },
        profile="test",
        config_dir=None,
        resource_dir=tmp_path,
    )
    app = create_app(config)
    with TestClient(app) as client:
        assert client.get("/actuator/health/readiness").status_code == 200

        async def unavailable():
            return False

        monkeypatch.setattr(app.state.auth_service.cache, "readiness", unavailable)
        assert client.get("/actuator/health/readiness").status_code == 503
        assert client.get("/actuator/health").status_code == 200

        async def auth_unavailable(_token):
            raise AuthError("backend unavailable", 503, "temporarily_unavailable")

        monkeypatch.setattr(app.state.auth_service, "userinfo", auth_unavailable)
        response = client.get("/oauth2/userinfo", headers={"Authorization": "Bearer example"})
        assert response.status_code == 503
        assert response.json()["success"] is False


def test_production_app_cannot_start_with_disabled_redis(tmp_path: Path):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    pem = key.private_bytes(
        serialization.Encoding.PEM,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    ).decode()
    config = AppConfig(
        {
            "integrations": {
                "database": {"url": f"sqlite+aiosqlite:///{tmp_path / 'startup.db'}"},
                "redis": {"enabled": False},
            },
            "jbm": {"auth": {"jwt": {"issuer": "https://auth.example", "private-key": pem}}},
        },
        profile="prod",
        config_dir=None,
    )
    with pytest.raises(RuntimeError, match="shared Redis"), TestClient(create_app(config)):
        pytest.fail("Production startup unexpectedly accepted per-process sessions")
