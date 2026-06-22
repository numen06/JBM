from __future__ import annotations

import base64
import json
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import httpx
from fastapi.testclient import TestClient
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.main import create_app
from jbm_cluster_py.platform.auth.repository import SQLITE_DDL
from jbm_cluster_py.platform.auth.service import AuthService, TokenCache, _secret_matches


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


def test_image_captcha_verify_is_case_insensitive_like_java() -> None:
    async def run() -> None:
        cache = TokenCache(RedisClient({"enabled": False}))
        service = AuthService(None, cache, {})  # type: ignore[arg-type]
        await cache.set_json("captcha:system:ab12c", {"code": "AB12C"}, 60)
        assert await service.verify_captcha("aB12c") is True

    import asyncio

    asyncio.run(run())


def test_java_app_secret_codec_encrypted_secret_is_supported() -> None:
    plain = b"plain-client-secret"
    aes_key = __import__("hashlib").md5(b"jbm-app-client-secret").hexdigest().encode("utf-8")
    pad_len = 16 - (len(plain) % 16)
    encryptor = Cipher(algorithms.AES(aes_key), modes.ECB()).encryptor()
    encrypted = encryptor.update(plain + bytes([pad_len]) * pad_len) + encryptor.finalize()
    stored = "$ENC$" + base64.b64encode(encrypted).decode("ascii")
    assert _secret_matches("plain-client-secret", stored) is True
    assert _secret_matches("wrong", stored) is False


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

        public_login = client.post(
            "/oauth2/token",
            data={
                "grant_type": "password",
                "client_id": "JBM",
                "username": "admin",
                "password": "admin123",
                "scope": "all",
            },
        ).json()
        assert public_login["success"] is True
        public_token = public_login["result"]
        public_refresh = client.post(
            "/oauth2/refresh",
            data={
                "client_id": "JBM",
                "refresh_token": public_token["refresh_token"],
            },
        ).json()
        assert public_refresh["success"] is True
        assert public_refresh["result"]["access_token"].count(".") == 2

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

        missing_secret_client_token = client.post(
            "/oauth2/token",
            data={
                "grant_type": "client_credentials",
                "client_id": "JBM",
                "scope": "all",
            },
        ).json()
        assert missing_secret_client_token["success"] is False
        assert missing_secret_client_token["code"] == 401

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


def test_authorization_code_page_login_and_exchange(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-code.db")
    import asyncio

    asyncio.run(seed_database(database_url))

    with TestClient(create_app(auth_config(database_url))) as client:
        authorize_page = client.get(
            "/oauth2/authorize",
            params={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "scope": "all",
                "state": "state-1",
            },
        )
        assert authorize_page.status_code == 200
        assert "JBM 认证中心" in authorize_page.text
        assert 'name="client_id" value="JBM"' in authorize_page.text

        apps = client.get("/oauth2/apps").json()
        assert apps["success"] is True
        assert apps["result"][0]["clientId"] == "JBM"
        assert "apiKey" not in apps["result"][0]
        assert "clientSecret" not in apps["result"][0]

        login = client.post(
            "/oauth2/doLogin",
            data={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "scope": "all",
                "state": "state-1",
                "username": "admin",
                "password": "admin123",
            },
        ).json()
        assert login["success"] is True
        parsed = urlparse(login["result"])
        assert parsed.scheme == "http"
        assert parsed.netloc == "admin.test"
        query = parse_qs(parsed.query)
        assert query["state"] == ["state-1"]
        code = query["code"][0]

        token_response = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "redirect_uri": "http://admin.test/login/callback",
                "code": code,
            },
        ).json()
        assert token_response["success"] is True
        assert token_response["result"]["access_token"].count(".") == 2
        assert token_response["result"]["login_id"] == "normal:1000:2057849052900044802"

        reused = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "redirect_uri": "http://admin.test/login/callback",
                "code": code,
            },
        ).json()
        assert reused["success"] is False
        assert reused["code"] == 401

        second_login = client.post(
            "/oauth2/doLogin",
            data={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "scope": "all",
                "username": "admin",
                "password": "admin123",
            },
        ).json()
        second_code = parse_qs(urlparse(second_login["result"]).query)["code"][0]
        public_client_exchange = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": second_code,
            },
        ).json()
        assert public_client_exchange["success"] is True
        assert public_client_exchange["result"]["access_token"].count(".") == 2


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


def test_register_captcha_and_qrcode_frontend_paths(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-register.db")
    import asyncio

    public_key, private_key = rsa_pair_base64()
    asyncio.run(seed_database(database_url, public_key, private_key))
    loaded_public = serialization.load_der_public_key(base64.b64decode(public_key))
    encrypted_password = base64.b64encode(loaded_public.encrypt(b"newpass123", padding.PKCS1v15())).decode("ascii")

    with TestClient(create_app(auth_config(database_url))) as client:
        captcha = client.get("/captcha/vcode64", params={"width": 120, "height": 40}).json()
        assert captcha["success"] is True
        assert captcha["result"].startswith("data:image/png;base64,")

        registered = client.post(
            "/oauth2/register",
            headers={"X-Password-Encrypted": "true"},
            data={
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "userName": "newuser",
                "password": encrypted_password,
                "nickName": "New User",
                "vcode": "9999",
            },
        ).json()
        assert registered["success"] is True
        assert registered["result"]["userName"] == "newuser"

        logged_in = client.post(
            "/oauth2/token",
            data={
                "grant_type": "password",
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "username": "newuser",
                "password": "newpass123",
                "vcode": "9999",
            },
        ).json()
        assert logged_in["success"] is True
        assert logged_in["result"]["access_token"].count(".") == 2

        qr = client.get(
            "/qrcode/login",
            params={"client_id": "JBM", "redirect_uri": "http://admin.test/login/callback", "width": 180, "height": 180},
        ).json()
        assert qr["success"] is True
        assert qr["result"]["image"].startswith("data:image/png;base64,")
        assert qr["result"]["scanUrl"].startswith("http://admin.test/qr-login?")
        waiting = client.get("/qrcode/check", params={"code": qr["result"]["code"]}).json()
        assert waiting["success"] is False
        assert waiting["result"] == 0


def test_qrcode_confirm_returns_authorization_code(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-qr.db")
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
        ).json()
        access_token = token_response["result"]["access_token"]

        qr = client.get(
            "/qrcode/login",
            params={"client_id": "JBM", "redirect_uri": "http://admin.test/login/callback"},
        ).json()["result"]
        code = qr["code"]

        scanned = client.get("/qrcode/scanned", params={"code": code}).json()
        assert scanned["success"] is True
        assert scanned["result"] == 1

        waiting = client.get("/qrcode/check", params={"code": code}).json()
        assert waiting["success"] is False
        assert waiting["result"] == 1

        confirmed = client.post(
            "/qrcode/confirm",
            params={"code": code},
            headers={"Authorization": "Bearer " + access_token},
        ).json()
        assert confirmed["success"] is True
        assert confirmed["result"]["code"]
        assert confirmed["result"]["redirectUri"] == "http://admin.test/login/callback"

        checked = client.get("/qrcode/check", params={"code": code}).json()
        assert checked["success"] is True
        assert checked["result"]["code"] == confirmed["result"]["code"]

        exchanged = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": checked["result"]["code"],
            },
        ).json()
        assert exchanged["success"] is True
        assert exchanged["result"]["access_token"].count(".") == 2
        assert exchanged["result"]["login_id"] == "normal:1000:2057849052900044802"


def test_phone_code_is_sent_through_push_notification() -> None:
    import asyncio

    requests: list[dict[str, object]] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(dict(request.url.params))
        return httpx.Response(200, json={"success": True, "result": {"Code": "OK", "pin": request.url.params["code"]}})

    async def run() -> bool:
        client = httpx.AsyncClient(transport=httpx.MockTransport(handler), base_url="http://push.test")
        service = AuthService(
            repository=None,  # type: ignore[arg-type]
            cache=TokenCache(RedisClient({"enabled": False})),
            config={"sms": {"push-base-url": "http://push.test"}},
            http_client=client,
        )
        try:
            return await service.send_phone_code("13585658904", "9999")
        finally:
            await client.aclose()

    assert asyncio.run(run()) is True
    assert requests[0]["phoneNumber"] == "13585658904"
    assert requests[0]["templateCode"] == "SMS_236340338"
    assert requests[0]["signName"] == "甲佳智能"
    assert requests[0]["code"]


def test_phone_code_accepts_jaja7_dry_run_sms_delivery() -> None:
    import asyncio

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            200,
            json={
                "success": True,
                "result": {
                    "Code": "OK",
                    "Message": "jaja7 dry-run: SMS not sent",
                    "pin": request.url.params["code"],
                },
            },
        )

    async def run() -> bool:
        client = httpx.AsyncClient(transport=httpx.MockTransport(handler), base_url="http://push.test")
        service = AuthService(
            repository=None,  # type: ignore[arg-type]
            cache=TokenCache(RedisClient({"enabled": False})),
            config={"sms": {"push-base-url": "http://push.test"}},
            http_client=client,
        )
        try:
            return await service.send_phone_code("13585658904", "9999")
        finally:
            await client.aclose()

    assert asyncio.run(run()) is True
