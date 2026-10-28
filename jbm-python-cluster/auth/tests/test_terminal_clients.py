"""Explicit mobile registrations and the actual HTTP authorization-code flow."""

import asyncio
import importlib.util
import json
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from alembic.migration import MigrationContext
from alembic.operations import Operations
from fastapi.testclient import TestClient
from jbm_cluster_py.platform.auth.main import create_app
from jbm_cluster_py.platform.auth.repository import AuthRepository
from sqlalchemy import create_engine, text
from test_auth_compat import PKCE_CHALLENGE, PKCE_VERIFIER, auth_config, seed_database


def test_mobile_registration_migration_is_idempotent_and_preserves_settings(tmp_path):
    path = (
        Path(__file__).resolve().parents[2]
        / "center/migrations/versions/20261029_27_terminal_sessions.py"
    )
    spec = importlib.util.spec_from_file_location("terminal_migration", path)
    migration = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(migration)
    engine = create_engine(f"sqlite:///{tmp_path / 'migration.db'}")
    with engine.begin() as conn:
        conn.execute(
            text(
                "CREATE TABLE base_app (app_id INTEGER PRIMARY KEY, api_key TEXT, extend_data TEXT)"
            )
        )
        original = {
            "other": {"keep": True},
            "oauth": {"publicClient": False, "terminalClients": {"app": {"enabled": False}}},
        }
        conn.execute(
            text("INSERT INTO base_app VALUES (20, :key, :data)"),
            {"key": migration.CLIENT_ID, "data": json.dumps(original)},
        )
        with Operations.context(MigrationContext.configure(conn)):
            migration.upgrade()
            first = conn.execute(text("SELECT extend_data FROM base_app")).scalar_one()
            migration.upgrade()
            assert conn.execute(text("SELECT extend_data FROM base_app")).scalar_one() == first
        data = json.loads(first)
        assert data["other"] == original["other"]
        assert data["oauth"]["publicClient"] is False
        assert data["oauth"]["terminalClients"]["app"] == {"enabled": False}
        assert data["oauth"]["terminalClients"]["miniapp"] == {
            "enabled": True,
            "redirectUris": ["/login/callback"],
        }
    engine.dispose()


def test_registered_clients_share_app_permissions_but_not_sessions(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'clients.db'}"

    async def prepare():
        await seed_database(url)
        repo = AuthRepository({"url": url})
        assert await repo.find_client("JBM:miniapp") is None
        async with repo.engine.begin() as conn:
            row = json.loads(
                (
                    await conn.execute(text("SELECT extend_data FROM base_app WHERE api_key='JBM'"))
                ).scalar_one()
            )
            row["oauth"]["terminalClients"] = {
                t: {"enabled": True, "redirectUris": ["/login/callback"]}
                for t in ("miniapp", "app")
            }
            await conn.execute(
                text("UPDATE base_app SET extend_data=:data WHERE api_key='JBM'"),
                {"data": json.dumps(row)},
            )
        parent = await repo.find_client("JBM")
        for terminal in ("miniapp", "app"):
            registered = await repo.find_client("JBM:" + terminal)
            assert registered["appId"] == parent["appId"] == 1000
            assert registered["publicClient"] is True
            assert registered["clientSecret"] is None
            assert registered["terminalType"] == terminal
        assert await repo.find_client("JBM:android") is None
        assert await repo.find_client("JBM:app:miniapp") is None
        await repo.stop()

    asyncio.run(prepare())
    with TestClient(create_app(auth_config(url))) as client:

        def login(client_id):
            redirect_uri = (
                "http://admin.test/login/callback" if client_id == "JBM" else "/login/callback"
            )
            response = client.post(
                "/oauth2/doLogin",
                data={
                    "response_type": "code",
                    "client_id": client_id,
                    "redirect_uri": redirect_uri,
                    "username": "admin",
                    "password": "admin123",
                    "state": "test-state",
                    "code_challenge": PKCE_CHALLENGE,
                    "code_challenge_method": "S256",
                    # Untrusted terminal hints must not change the registered type.
                    "terminalType": "app",
                    "deviceType": "app",
                },
            ).json()
            assert response["success"] is True, response
            params = parse_qs(urlparse(response["result"]).query)
            assert params["state"] == ["test-state"]
            token_response = client.post(
                "/oauth2/token",
                data={
                    "grant_type": "authorization_code",
                    "client_id": client_id,
                    "redirect_uri": redirect_uri,
                    "code": params["code"][0],
                    "code_verifier": PKCE_VERIFIER,
                },
            )
            assert token_response.status_code == 200, token_response.json()
            return token_response.json()

        def identity(token):
            return client.get(
                "/oauth2/userinfo", headers={"Authorization": "Bearer " + token["access_token"]}
            ).json()

        tokens = {
            terminal: login("JBM" + (":" + terminal if terminal != "web" else ""))
            for terminal in ("web", "miniapp", "app")
        }
        for terminal, token in tokens.items():
            assert token["terminal_type"] == terminal
            user = identity(token)
            assert user["success"] is True
            assert "ACTION_SAVE" in user["result"]["permissions"]
        replacement = login("JBM:miniapp")
        assert identity(tokens["miniapp"])["success"] is False
        assert identity(replacement)["success"] is True
        for terminal in ("web", "app"):
            assert identity(tokens[terminal])["success"] is True
        assert (
            client.post(
                "/oauth2/token",
                data={
                    "grant_type": "refresh_token",
                    "client_id": "JBM:miniapp",
                    "refresh_token": tokens["miniapp"]["refresh_token"],
                },
            ).json()["error"]
            == "invalid_grant"
        )
        renewed = client.post(
            "/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "JBM:app",
                "refresh_token": tokens["app"]["refresh_token"],
            },
        ).json()
        assert renewed["refresh_expires_in"] == 2592000
        assert renewed["refresh_token"] != tokens["app"]["refresh_token"]
        assert (
            client.post("/oauth2/logout", data={"access_token": renewed["access_token"]}).json()[
                "success"
            ]
            is True
        )
        assert identity(renewed)["success"] is False
        assert identity(tokens["web"])["success"] is True


def test_malformed_or_disabled_mobile_registration_fails_closed(tmp_path):
    url = f"sqlite+aiosqlite:///{tmp_path / 'disabled.db'}"

    async def exercise():
        await seed_database(url)
        repo = AuthRepository({"url": url})
        for value in (
            [],
            None,
            {"app": None},
            {"app": {"enabled": "true", "redirectUris": ["/login/callback"]}},
            {"app": {"enabled": False}},
            {"app": {"enabled": True, "redirectUris": "*"}},
        ):
            async with repo.engine.begin() as conn:
                await conn.execute(
                    text("UPDATE base_app SET extend_data=:data WHERE api_key='JBM'"),
                    {"data": json.dumps({"oauth": {"terminalClients": value}})},
                )
            assert await repo.find_client("JBM:app") is None
        async with repo.engine.begin() as conn:
            await conn.execute(text("UPDATE base_app SET status=0"))
        assert await repo.find_client("JBM:miniapp") is None
        await repo.stop()

    asyncio.run(exercise())
