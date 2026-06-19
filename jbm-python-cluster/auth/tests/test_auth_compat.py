from __future__ import annotations

import base64
from pathlib import Path

from fastapi.testclient import TestClient
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.auth.main import create_app
from jbm_cluster_py.platform.auth.repository import SQLITE_DDL


def auth_config(database_url: str) -> AppConfig:
    return AppConfig(
        {
            "server": {"host": "127.0.0.1", "port": 5555},
            "spring": {
                "application": {"name": "jbm-cluster-platform-auth"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
                "datasource": {"url": database_url},
            },
            "integrations": {"redis": {"enabled": False}, "telemetry": {"enabled": False}},
            "jbm": {
                "auth": {
                    "account-domain": "@admin.com",
                    "jwt": {
                        "issuer": "http://auth.test",
                        "audience": "jbm-api",
                        "access-token-seconds": 3600,
                        "refresh-token-seconds": 86400,
                    },
                }
            },
        },
        profile="test",
        config_dir=None,
        app="auth",
    )


def rsa_pair_base64() -> tuple[str, str]:
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    private_der = private_key.private_bytes(
        serialization.Encoding.DER,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    )
    public_der = private_key.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    return base64.b64encode(public_der).decode("ascii"), base64.b64encode(private_der).decode("ascii")


async def seed_database(database_url: str, public_key: str = "PUBLIC-KEY", private_key: str = "") -> None:
    engine = create_async_engine(database_url)
    async with engine.begin() as conn:
        for ddl in SQLITE_DDL:
            await conn.execute(text(ddl))
        await conn.execute(
            text(
                """
                INSERT INTO base_app (app_id, api_key, secret_key, app_type, status, public_key)
                VALUES (1000, 'JBM', 'demo-secret', 'pc', 1, :public_key)
                """
            ),
            {"public_key": public_key},
        )
        await conn.execute(
            text("UPDATE base_app SET private_key = :private_key WHERE api_key = 'JBM'"),
            {"private_key": private_key},
        )
        await conn.execute(
            text(
                """
                INSERT INTO base_user
                  (user_id, user_name, user_type, nick_name, real_name, email, mobile, status)
                VALUES
                  (2057849052900044802, 'admin', 'normal', 'Admin', 'Administrator',
                   'admin@example.com', '13800000000', 1)
                """
            )
        )
        await conn.execute(
            text(
                """
                INSERT INTO base_account
                  (account_id, user_id, account, password, account_type, status, domain, must_change_password)
                VALUES
                  (1, 2057849052900044802, 'admin', 'admin123', 'username', 1, '@admin.com', 0)
                """
            )
        )
        await conn.execute(
            text(
                """
                INSERT INTO base_role (role_id, role_code, role_name, status, parent_id)
                VALUES (2, 'ops', 'Ops', 1, NULL)
                """
            )
        )
        await conn.execute(text("INSERT INTO base_role_user (user_id, role_id) VALUES (2057849052900044802, 2)"))
        await conn.execute(
            text(
                """
                INSERT INTO base_authority (authority_id, authority, resource_type, api_id, status)
                VALUES
                  (10, 'MENU_DASHBOARD', 'menu', NULL, 1),
                  (11, 'ACTION_SAVE', 'action', NULL, 1),
                  (12, 'API_INTERNAL', 'api', 1, 1),
                  (13, 'ACTION_monitor:online:forceLogout', 'action', NULL, 1)
                """
            )
        )
        await conn.execute(text("INSERT INTO base_authority_role (authority_id, role_id) VALUES (11, 2), (13, 2)"))
    await engine.dispose()


def test_password_refresh_userinfo_and_client_credentials(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth.db")
    import asyncio

    asyncio.run(seed_database(database_url))

    with TestClient(create_app(auth_config(database_url))) as client:
        token_response = client.post(
            "/oauth2/token",
            data={
                "grant_type": "password",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "username": "admin",
                "password": "admin123",
                "scope": "all",
            },
        )
        assert token_response.status_code == 200
        token_body = token_response.json()
        assert token_body["success"] is True
        token = token_body["result"]
        assert token["token_type"] == "Bearer"
        assert token["access_token"].count(".") == 2
        assert token["refresh_token"]
        assert token["login_id"] == "normal:1000:2057849052900044802"
        assert "ops" in token["roles"]
        assert "ACTION_SAVE" in token["permissions"]
        assert "ACTION_monitor:online:forceLogout" in token["permissions"]

        userinfo = client.get("/oauth2/userinfo", headers={"Authorization": "Bearer " + token["access_token"]})
        assert userinfo.json()["result"]["userId"] == 2057849052900044802
        assert userinfo.json()["result"]["clientId"] == "JBM"

        online = client.post(
            "/online/pageList",
            json={"pageForm": {"currPage": 1, "pageSize": 10}},
            headers={"Authorization": "Bearer " + token["access_token"]},
        )
        online_body = online.json()
        assert online_body["success"] is True
        assert online_body["result"]["total"] == 1
        assert online_body["result"]["contents"][0]["userName"] == "admin"

        refreshed = client.post(
            "/oauth2/refresh",
            data={
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "refresh_token": token["refresh_token"],
            },
        )
        assert refreshed.json()["success"] is True
        assert refreshed.json()["result"]["access_token"].count(".") == 2

        client_token = client.post(
            "/oauth2/token",
            data={
                "grant_type": "client_credentials",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "scope": "all",
            },
        )
        assert client_token.json()["success"] is True
        assert "refresh_token" not in client_token.json()["result"]

        denied = client.delete(
            "/online/kickout/not-a-real-token",
            headers={"Authorization": "Bearer " + client_token.json()["result"]["access_token"]},
        )
        assert denied.json()["success"] is False
        assert denied.json()["code"] == 403

        allowed = client.delete(
            "/online/kickout/not-a-real-token",
            headers={"Authorization": "Bearer " + token["access_token"]},
        )
        assert allowed.json()["success"] is True

        well_known = client.get("/.well-known/openid-configuration").json()
        assert well_known["issuer"] == "http://auth.test"
        assert "jwks_uri" in well_known
        assert client.get("/jwks.json").json()["keys"][0]["alg"] == "RS256"


def test_rsa_encrypted_password_is_supported(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-rsa.db")
    import asyncio

    public_key, private_key = rsa_pair_base64()
    asyncio.run(seed_database(database_url, public_key, private_key))

    loaded_public = serialization.load_der_public_key(base64.b64decode(public_key))
    encrypted_password = base64.b64encode(loaded_public.encrypt(b"admin123", padding.PKCS1v15())).decode("ascii")

    with TestClient(create_app(auth_config(database_url))) as client:
        public_key_response = client.get("/oauth2/publicKey", params={"app_id": "JBM"}).json()
        assert public_key_response["result"] == public_key

        token_response = client.post(
            "/oauth2/token",
            headers={"X-Password-Encrypted": "true"},
            data={
                "grant_type": "password",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "username": "admin",
                "password": encrypted_password,
                "scope": "all",
            },
        ).json()
        assert token_response["success"] is True
        assert token_response["result"]["access_token"].count(".") == 2


def test_password_error_lockout_message(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-lock.db")
    import asyncio

    asyncio.run(seed_database(database_url))

    with TestClient(create_app(auth_config(database_url))) as client:
        for _ in range(5):
            failed = client.post(
                "/oauth2/token",
                data={
                    "grant_type": "password",
                    "client_id": "JBM",
                    "client_secret": "demo-secret",
                    "username": "admin",
                    "password": "bad",
                },
            ).json()
            assert failed["success"] is False
            assert failed["message"] == "用户名或密码错误"

        locked = client.post(
            "/oauth2/token",
            data={
                "grant_type": "password",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "username": "admin",
                "password": "admin123",
            },
        ).json()
        assert locked["success"] is False
        assert locked["message"] == "密码错误次数过多，帐户锁定10分钟"
