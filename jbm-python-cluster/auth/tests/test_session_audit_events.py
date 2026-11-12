"""Real database audit coverage for login completion and session lifecycle events."""

import asyncio
import json

from fastapi.testclient import TestClient
from jbm_cluster_py.platform.auth.main import create_app
from test_auth_compat import auth_config, capture_audit, seed_database


def test_login_refresh_failure_replacement_and_logout_are_durable_and_redacted(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'audit.db'}"
    asyncio.run(seed_database(url))
    with TestClient(create_app(auth_config(url))) as client:
        audit = capture_audit(client)
        form = {
            "grant_type": "password",
            "client_id": "JBM",
            "username": "admin",
            "password": "admin123",
        }
        first = client.post("/oauth2/token", data=form).json()
        assert "access_token" in first
        current = client.post(
            "/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "JBM",
                "refresh_token": first["refresh_token"],
            },
        ).json()
        assert current["access_token"] != first["access_token"]
        assert client.post('/oauth2/refresh', data={
            'client_id': 'JBM', 'refresh_token': 'invalid-refresh-alias',
        }).status_code == 400
        second = client.post("/oauth2/token", data=form).json()
        assert "access_token" in second
        assert (
            client.post(
                "/oauth2/token",
                data={
                    "grant_type": "refresh_token",
                    "client_id": "JBM",
                    "refresh_token": current["refresh_token"],
                },
            ).status_code
            == 400
        )
        assert (
            client.post(
                "/oauth2/token",
                data={
                    "grant_type": "authorization_code",
                    "client_id": "JBM",
                    "code": "invalid-test-code",
                },
            ).status_code
            == 400
        )
        assert (
            client.post(
                "/oauth2/logout",
                data={
                    "access_token": second["access_token"],
                    "refresh_token": second["refresh_token"],
                },
            ).status_code
            == 200
        )
        bad = client.post("/oauth2/token", data={**form, "password": "WRONG-SUPERSECRET"})
        assert bad.json()["captcha_required"] is True
        assert client.post("/oauth2/token", data=form).json()["captcha_required"] is True

        logs = list(audit.events)
        types = {row["loginType"] for row in logs}
        assert {
            "LOGIN:PASSWORD",
            "REFRESH",
            "REFRESH_FAILED",
            "AUTH_CODE_FAILED",
            "SESSION_REPLACED",
            "LOGOUT",
        } <= types
        for row in logs:
            assert row["clientId"] == "JBM"
            assert row["requestId"]
            assert row["ip"]
        assert len([row for row in logs if row["loginType"] == "LOGOUT"]) == 1
        encoded = json.dumps(logs, default=str)
        for value in (
            "admin123",
            "WRONG-SUPERSECRET",
            first["access_token"],
            first["refresh_token"],
            current["refresh_token"],
            second["access_token"],
        ):
            assert value not in encoded
        completed = [row for row in logs if row["loginType"].startswith("LOGIN:")]
        assert len(completed) == 2
        assert all(row["userId"] == 2057849052900044802 for row in completed)

        third = client.post('/oauth2/token', data=form).json()
        # The previous failed attempt now requires the existing test CAPTCHA.
        if 'access_token' not in third:
            third = client.post('/oauth2/token', data={**form, 'vcode': '9999'}).json()
        assert client.post('/oauth2/revoke', data={
            'client_id': 'JBM', 'token': third['refresh_token'], 'token_type_hint': 'refresh_token',
        }).status_code == 200
        assert any(row['loginType'] == 'REVOKE' for row in audit.events)


def test_early_login_client_failure_is_not_silently_omitted(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'invalid.db'}"
    asyncio.run(seed_database(url))
    with TestClient(create_app(auth_config(url))) as client:
        audit = capture_audit(client)
        response = client.post(
            "/oauth2/doLogin",
            data={
                "response_type": "code",
                "client_id": "not-registered",
                "username": "someone",
                "password": "NEVER-LOG-THIS",
                "redirect_uri": "/callback",
            },
        )
        assert response.status_code == 401

        rows = list(audit.events)
        assert len(rows) == 1
        assert rows[0]["loginType"] == "LOGIN_FAILED"
        assert rows[0]["account"] == "someone"
        assert rows[0]["success"] is False
        assert "NEVER-LOG-THIS" not in json.dumps(rows, default=str)
