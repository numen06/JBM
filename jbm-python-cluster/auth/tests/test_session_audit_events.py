"""Real database audit coverage for login completion and session lifecycle events."""

import asyncio
import json

from fastapi.testclient import TestClient
from jbm_cluster_py.platform.auth.main import create_app
from sqlalchemy import text
from test_auth_compat import auth_config, seed_database


def test_login_refresh_failure_replacement_and_logout_are_durable_and_redacted(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'audit.db'}"
    asyncio.run(seed_database(url))
    with TestClient(create_app(auth_config(url))) as client:
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

        async def read():
            async with client.app.state.repository.engine.connect() as connection:
                return [
                    dict(row)
                    for row in (
                        await connection.execute(
                            text("SELECT * FROM base_account_logs ORDER BY account_log_id")
                        )
                    ).mappings()
                ]

        logs = client.portal.call(read)
        types = {row["login_type"] for row in logs}
        assert {
            "LOGIN:PASSWORD",
            "REFRESH",
            "REFRESH_FAILED",
            "AUTH_CODE_FAILED",
            "SESSION_REPLACED",
            "LOGOUT",
        } <= types
        for row in logs:
            assert row["client_id"] == "JBM"
            assert row["request_id"]
            assert row["ip"]
        assert len([row for row in logs if row["login_type"] == "LOGOUT"]) == 1
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
        completed = [row for row in logs if row["login_type"].startswith("LOGIN:")]
        assert len(completed) == 2
        assert all(row["user_id"] == 2057849052900044802 for row in completed)


def test_early_login_client_failure_is_not_silently_omitted(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'invalid.db'}"
    asyncio.run(seed_database(url))
    with TestClient(create_app(auth_config(url))) as client:
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

        async def read():
            async with client.app.state.repository.engine.connect() as connection:
                return [
                    dict(row)
                    for row in (
                        await connection.execute(text("SELECT * FROM base_account_logs"))
                    ).mappings()
                ]

        rows = client.portal.call(read)
        assert len(rows) == 1
        assert rows[0]["login_type"] == "LOGIN_FAILED"
        assert rows[0]["account"] == "someone"
        assert rows[0]["status"] == 0
        assert "NEVER-LOG-THIS" not in json.dumps(rows, default=str)
