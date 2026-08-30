from __future__ import annotations

import base64
import hashlib
import json
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import httpx
import pytest
from fastapi.testclient import TestClient
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.main import _validate_production_auth_config, create_app
from jbm_cluster_py.platform.auth.repository import AuthRepository, SQLITE_DDL, _client_oauth_settings
from jbm_cluster_py.platform.auth.service import AuthError, AuthService, TokenCache, _secret_matches, _timestamp


def test_client_oauth_settings_accepts_mysql_json_mapping() -> None:
    settings = _client_oauth_settings(
        {
            "website": "http://localhost:5173/login/callback",
            "extend_data": {
                "oauth": {
                    "publicClient": True,
                    "redirectUris": ["http://debug5173.feige.51jbm.cn/login/callback"],
                }
            },
        }
    )

    assert settings == {
        "publicClient": True,
        "redirectUris": ["http://debug5173.feige.51jbm.cn/login/callback"],
    }


def test_registered_same_origin_redirect_is_address_independent() -> None:
    service = object.__new__(AuthService)
    service.require_https_redirects = True
    client = {"redirectUris": ["/login/callback"]}

    service._validate_redirect_uri(client, "/login/callback")
    service._validate_redirect_uri(client, "/login/callback?provider=mock")
    with pytest.raises(AuthError, match="未登记"):
        service._validate_redirect_uri(client, "//evil.example/login/callback")
    with pytest.raises(AuthError, match="未登记"):
        service._validate_redirect_uri(client, "/other/callback")


def test_online_session_timestamps_are_timezone_aware() -> None:
    assert _timestamp(0) == "1970-01-01T00:00:00Z"


def test_roles_are_scoped_by_tenant_and_app(tmp_path: Path) -> None:
    import asyncio

    async def exercise() -> None:
        database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "role-scope.db")
        repository = AuthRepository({"url": database_url})
        await repository.start()
        async with repository.engine.begin() as conn:
            await conn.execute(
                text(
                    "INSERT INTO base_role (role_id, app_id, role_code, role_name, status) VALUES "
                    "(10, 3000, 'iot_admin', 'IoT Admin', 1), "
                    "(11, 4000, 'building_admin', 'Building Admin', 1)"
                )
            )
            await conn.execute(
                text(
                    "INSERT INTO base_role_user (user_id, role_id, app_id, tenant_id) VALUES "
                    "(7, 10, 3000, 100), (7, 10, 3000, 200), (7, 11, 4000, 100)"
                )
            )

        tenant_100_iot = await repository.user_roles(7, 3000, 100)
        tenant_200_iot = await repository.user_roles(7, 3000, 200)
        tenant_100_building = await repository.user_roles(7, 4000, 100)
        wrong_tenant = await repository.user_roles(7, 3000, 300)

        assert [row["role_code"] for row in tenant_100_iot] == ["iot_admin"]
        assert [row["role_code"] for row in tenant_200_iot] == ["iot_admin"]
        assert [row["role_code"] for row in tenant_100_building] == ["building_admin"]
        assert wrong_tenant == []
        await repository.stop()

    asyncio.run(exercise())


def test_user_authorities_supports_legacy_relation_tables_without_app_id(tmp_path: Path) -> None:
    import asyncio

    async def exercise() -> None:
        database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "legacy-authorities.db")
        repository = AuthRepository({"url": database_url})
        await repository.start()
        async with repository.engine.begin() as conn:
            await conn.execute(
                text(
                    "INSERT INTO base_role (role_id, app_id, role_code, role_name, status) "
                    "VALUES (20, 3000, 'tenant_admin', 'Tenant Admin', 1)"
                )
            )
            await conn.execute(
                text(
                    "INSERT INTO base_role_user (user_id, role_id, app_id, tenant_id) "
                    "VALUES (8, 20, 3000, 100)"
                )
            )
            await conn.execute(
                text(
                    "INSERT INTO base_authority (authority_id, app_id, authority, resource_type, status) "
                    "VALUES (21, 3000, 'ACTION_TENANT_READ', 'action', 1)"
                )
            )
            await conn.execute(
                text("INSERT INTO base_authority_role (authority_id, role_id) VALUES (21, 20)")
            )

        assert await repository.user_authorities(8, False, 3000, 100) == ["ACTION_TENANT_READ"]
        await repository.stop()

    asyncio.run(exercise())


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
                    "require-https-redirects": False,
                    "dev-bypass-enabled": True,
                    "allow-plaintext-secrets": True,
                    "legacy-password-grant-enabled": True,
                    "login-providers": {
                        "face": {
                            "enabled": True,
                            "dev-mock-enabled": True,
                            "dev-subject": "13800000000",
                            "account-type": "face",
                        },
                        "wechat": {
                            "enabled": True,
                            "dev-mock-enabled": True,
                            "dev-code": "wechat-test-code",
                            "dev-subject": "wechat-openid",
                            "account-type": "wechat",
                        },
                        "miniapp": {
                            "enabled": True,
                            "dev-mock-enabled": True,
                            "dev-code": "miniapp-test-code",
                            "dev-subject": "13800000000",
                            "account-type": "miniapp",
                        },
                        "thirdparty-mock": {
                            "enabled": True,
                            "dev-mock-enabled": True,
                            "dev-code": "thirdparty-test-code",
                            "dev-subject": "mock-subject",
                            "account-type": "thirdparty_mock",
                            "require-subject-match": False,
                        },
                    },
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


PKCE_VERIFIER = "jbm-test-pkce-verifier-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ"
PKCE_CHALLENGE = base64.urlsafe_b64encode(
    hashlib.sha256(PKCE_VERIFIER.encode("ascii")).digest()
).decode("ascii").rstrip("=")


def test_image_captcha_verify_is_case_insensitive_like_java() -> None:
    async def run() -> None:
        cache = TokenCache(RedisClient({"enabled": False}))
        service = AuthService(None, cache, {})  # type: ignore[arg-type]
        await cache.set_json("captcha:system:ab12c", {"code": "AB12C"}, 60)
        assert await service.verify_captcha("aB12c") is True
        with pytest.raises(AuthError, match="验证码错误"):
            await service.verify_captcha("aB12c")
        await cache.set_json("phone:13800000000", {"code": "123456"}, 60)
        assert await service.verify_phone_code("13800000000", "123456") is True
        with pytest.raises(AuthError, match="验证码错误"):
            await service.verify_phone_code("13800000000", "123456")

    import asyncio

    asyncio.run(run())


def test_login_captcha_can_be_required_for_password_and_sms() -> None:
    async def run(login_type: str) -> None:
        service = AuthService(
            None,
            TokenCache(RedisClient({"enabled": False})),
            {"login-captcha-required": True},
        )  # type: ignore[arg-type]
        with pytest.raises(AuthError, match="验证码不能为空"):
            await service._authenticate_user_for_client(
                {},
                {"loginType": login_type, "username": "tester", "password": "secret"},
            )

    import asyncio

    asyncio.run(run("PASSWORD"))
    asyncio.run(run("SMS"))


@pytest.mark.asyncio
async def test_permission_lookup_accepts_legacy_root_user_id_zero() -> None:
    class Repository:
        async def user_authorities(
            self, user_id: int, root: bool, app_id: int, tenant_id: int
        ) -> list[str]:
            assert (user_id, root, app_id, tenant_id) == (0, True, 1000, 2000)
            return ["ACTION_SAVE"]

    service = AuthService(
        Repository(),  # type: ignore[arg-type]
        TokenCache(RedisClient({"enabled": False})),
        {},
    )
    permissions = await service._permissions_for_claims(
        {"user_id": 0, "root": True, "app_id": 1000, "tenant_id": 2000}
    )
    assert permissions == ["ACTION_SAVE"]


def test_registration_creates_tenant_subscription_and_owner_role(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "tenant-registration.db")
    import asyncio

    asyncio.run(seed_database(database_url))

    async def enable_registration() -> None:
        engine = create_async_engine(database_url)
        async with engine.begin() as conn:
            await conn.execute(
                text(
                    "UPDATE base_app SET extend_data=:extend_data WHERE app_id=1000"
                ),
                {
                    "extend_data": json.dumps(
                        {
                            "oauth": {
                                "publicClient": True,
                                "redirectUris": ["http://admin.test/login/callback"],
                            },
                            "registration": {
                                "enabled": True,
                                "mode": "tenant",
                                "defaultRoleCode": "iot_admin",
                            },
                        }
                    )
                },
            )
            await conn.execute(
                text(
                    "INSERT INTO base_role "
                    "(role_id, app_id, role_code, role_name, status) "
                    "VALUES (3, 1000, 'iot_admin', 'IoT Admin', 1)"
                )
            )
        await engine.dispose()

    asyncio.run(enable_registration())

    async def register() -> dict[str, object]:
        repository = AuthRepository({"url": database_url})
        await repository.start()
        client = await repository.find_client("JBM")
        assert client and client["registration"]["defaultRoleCode"] == "iot_admin"
        result = await repository.create_tenant_account(
            app_id=1000,
            default_role_code="iot_admin",
            tenant_name="注册测试园区",
            org_type="company",
            username="tenant_owner",
            password_hash="hashed-password",
            nick_name="园区管理员",
            mobile="13800000017",
        )
        await repository.stop()
        return result

    result = asyncio.run(register())
    assert result["tenantName"] == "注册测试园区"
    assert result["roleCode"] == "iot_admin"

    async def verify() -> None:
        engine = create_async_engine(database_url)
        async with engine.connect() as conn:
            tenant = (
                await conn.execute(
                    text("SELECT * FROM base_org WHERE id=:tenant_id"),
                    {"tenant_id": result["tenantId"]},
                )
            ).mappings().one()
            user = (
                await conn.execute(
                    text("SELECT * FROM base_user WHERE user_id=:user_id"),
                    {"user_id": result["userId"]},
                )
            ).mappings().one()
            subscription = (
                await conn.execute(
                    text(
                        "SELECT * FROM base_tenant_app "
                        "WHERE tenant_id=:tenant_id AND app_id=1000"
                    ),
                    {"tenant_id": result["tenantId"]},
                )
            ).mappings().one()
            role = (
                await conn.execute(
                    text(
                        "SELECT r.role_code FROM base_role_user ru "
                        "JOIN base_role r ON r.role_id=ru.role_id "
                        "WHERE ru.user_id=:user_id AND ru.tenant_id=:tenant_id AND ru.app_id=1000"
                    ),
                    {"user_id": result["userId"], "tenant_id": result["tenantId"]},
                )
            ).one()
            mobile_account = (
                await conn.execute(
                    text(
                        "SELECT * FROM base_account "
                        "WHERE user_id=:user_id AND account='13800000017' AND account_type='mobile'"
                    ),
                    {"user_id": result["userId"]},
                )
            ).mappings().one()
        await engine.dispose()
        assert tenant["org_name"] == "注册测试园区"
        assert tenant["org_type"] == "company"
        assert user["company_id"] == result["tenantId"]
        assert user["mobile"] == "13800000017"
        assert mobile_account["status"] == 1
        assert subscription["status"] == 1
        assert role[0] == "iot_admin"

    asyncio.run(verify())


def test_public_registration_does_not_accept_identity_type() -> None:
    class RegistrationRepository:
        created: dict[str, object] = {}

        async def find_client(self, _client_id: str):
            return {
                "clientId": "iot-client",
                "appId": 1000,
                "registration": {"enabled": True, "mode": "tenant", "defaultRoleCode": "iot_admin"},
            }

        async def create_tenant_account(self, **kwargs):
            self.created = kwargs
            return {"tenantId": 1, "userId": 2, "appId": 1000, "roleCode": "iot_admin", **kwargs}

    async def run() -> None:
        repository = RegistrationRepository()
        cache = TokenCache(RedisClient({"enabled": False}))
        service = AuthService(repository, cache, {"dev-bypass-enabled": True})  # type: ignore[arg-type]
        await service.register(
            {
                "client_id": "iot-client",
                "userName": "new_account",
                "nickName": "测试账号",
                "password": "TestAccount@123!",
                "vcode": "9999",
                "organizationType": "organization",
                "tenantName": "客户端伪造公司",
            }
        )
        assert repository.created["org_type"] == "account"
        assert repository.created["tenant_name"] == "测试账号的账号空间"
        for field, value in (("mobile", "13800000018"), ("email", "user@example.com")):
            with pytest.raises(AuthError, match="个人中心验证绑定"):
                await service.register(
                    {
                        "client_id": "iot-client",
                        "userName": "blocked_identity",
                        "password": "TestAccount@123!",
                        "vcode": "9999",
                        field: value,
                    }
                )

    import asyncio

    asyncio.run(run())


def test_production_defaults_disable_bypass_and_password_grant() -> None:
    async def run() -> None:
        service = AuthService(None, TokenCache(RedisClient({"enabled": False})), {})  # type: ignore[arg-type]
        with pytest.raises(AuthError, match="验证码错误"):
            await service.verify_captcha("9999")
        with pytest.raises(AuthError, match="password grant已禁用"):
            await service.password_token({})

    import asyncio

    asyncio.run(run())

    with pytest.raises(RuntimeError, match="jwt.private-key"):
        _validate_production_auth_config(
            "prod",
            {
                "require-pkce": True,
                "require-https-redirects": True,
                "jwt": {"issuer": "https://auth.example.com"},
            },
        )


def test_fixed_captcha_does_not_enable_other_dev_bypasses() -> None:
    async def run() -> None:
        service = AuthService(
            None,
            TokenCache(RedisClient({"enabled": False})),
            {"fixed-captcha-code": "9999"},
        )  # type: ignore[arg-type]
        assert await service.verify_captcha("9999") is True
        with pytest.raises(AuthError, match="验证码错误"):
            await service.verify_phone_code("13800000000", "99999")

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
    assert _secret_matches("legacy-plain", "legacy-plain") is False
    assert _secret_matches("legacy-plain", "legacy-plain", allow_plaintext=True) is True


async def seed_database(database_url: str, public_key: str = "PUBLIC-KEY", private_key: str = "") -> None:
    engine = create_async_engine(database_url)
    async with engine.begin() as conn:
        for ddl in SQLITE_DDL:
            await conn.execute(text(ddl))
        await conn.execute(
            text(
                """
                INSERT INTO base_app
                  (app_id, api_key, secret_key, app_type, status, public_key, extend_data)
                VALUES
                  (1000, 'JBM', 'demo-secret', 'pc', 1, :public_key, :extend_data)
                """
            ),
            {
                "public_key": public_key,
                "extend_data": json.dumps(
                    {
                        "oauth": {
                            "publicClient": True,
                            "redirectUris": ["http://admin.test/login/callback"],
                        },
                        "registration": {
                            "enabled": True,
                            "mode": "tenant",
                            "defaultRoleCode": "ops",
                        },
                    }
                ),
            },
        )
        await conn.execute(
            text(
                """
                INSERT INTO base_account
                  (account_id, user_id, account, password, account_type, status, domain, must_change_password)
                VALUES
                  (2, 2057849052900044802, '13800000000', '', 'mobile', 1, '@admin.com', 0),
                  (3, 2057849052900044802, '13800000000', '', 'face', 1, '@admin.com', 0),
                  (4, 2057849052900044802, 'wechat-openid', '', 'wechat', 1, '@admin.com', 0),
                  (5, 2057849052900044802, '13800000000', '', 'miniapp', 1, '@admin.com', 0),
                  (6, 2057849052900044802, 'mock-subject', '', 'thirdparty_mock', 1, '@admin.com', 0)
                """
            )
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
        token = token_body
        assert token["token_type"] == "Bearer"
        assert token["access_token"].count(".") == 2
        assert token["refresh_token"]
        assert token["login_id"] == "normal:1000:2057849052900044802"
        assert "ops" in token["roles"]
        assert "ACTION_SAVE" in token["permissions"]
        assert "ACTION_monitor:online:forceLogout" in token["permissions"]
        payload_part = token["access_token"].split(".")[1]
        claims = json.loads(base64.urlsafe_b64decode(payload_part + "=" * (-len(payload_part) % 4)))
        assert "permissions" not in claims
        assert len(token["access_token"]) < 4096

        userinfo = client.get("/oauth2/userinfo", headers={"Authorization": "Bearer " + token["access_token"]})
        assert userinfo.json()["result"]["userId"] == 2057849052900044802
        assert userinfo.json()["result"]["clientId"] == "JBM"
        assert "tenantId" in userinfo.json()["result"]
        assert userinfo.json()["result"]["userType"] == "normal"
        assert "ACTION_SAVE" in userinfo.json()["result"]["permissions"]

        online = client.post(
            "/online/pageList",
            json={"pageForm": {"currPage": 1, "pageSize": 10}},
            headers={"Authorization": "Bearer " + token["access_token"]},
        )
        online_body = online.json()
        assert online_body["success"] is True
        assert online_body["result"]["total"] == 1
        assert online_body["result"]["contents"][0]["userName"] == "admin"
        assert online_body["result"]["contents"][0]["currentSession"] is True
        assert len(online_body["result"]["contents"][0]["tokenId"]) == 64
        assert online_body["result"]["contents"][0]["tokenId"] != token["access_token"]

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
        public_token = public_login
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
        assert "refresh_token" not in client_token.json()

        missing_secret_client_token = client.post(
            "/oauth2/token",
            data={
                "grant_type": "client_credentials",
                "client_id": "JBM",
                "scope": "all",
            },
        ).json()
        assert missing_secret_client_token["error"] == "invalid_client"

        denied = client.delete(
            "/online/kickout/not-a-real-token",
            headers={"Authorization": "Bearer " + client_token.json()["access_token"]},
        )
        assert denied.json()["success"] is False
        assert denied.json()["code"] == 403

        allowed = client.delete(
            "/online/kickout/" + online_body["result"]["contents"][0]["tokenId"],
            headers={"Authorization": "Bearer " + refreshed.json()["result"]["access_token"]},
        )
        assert allowed.json()["success"] is True

        well_known = client.get("/.well-known/openid-configuration").json()
        assert well_known["issuer"] == "http://auth.test"
        assert "jwks_uri" in well_known
        assert well_known["authorization_endpoint"].endswith("/oauth2/authorize")
        assert well_known["revocation_endpoint"].endswith("/oauth2/revoke")
        assert client.get("/.well-known/oauth-authorization-server").json() == well_known
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
        assert token_response["access_token"].count(".") == 2


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
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
            },
        )
        assert authorize_page.status_code == 200
        assert "JBM 认证中心" in authorize_page.text
        assert 'name="client_id" value="JBM"' in authorize_page.text
        assert 'action="/oauth2/authorize"' in authorize_page.text

        browser_submit = client.post(
            "/oauth2/authorize",
            data={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "scope": "all",
                "state": "browser-state",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
                "username": "admin",
                "password": "admin123",
            },
            follow_redirects=False,
        )
        assert browser_submit.status_code == 303
        assert parse_qs(urlparse(browser_submit.headers["location"]).query)["state"] == ["browser-state"]

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
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
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
                "code_verifier": PKCE_VERIFIER,
            },
        ).json()
        assert token_response["access_token"].count(".") == 2
        assert token_response["login_id"] == "normal:1000:2057849052900044802"

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
        assert reused["error"] == "invalid_grant"

        second_login = client.post(
            "/oauth2/doLogin",
            data={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "scope": "all",
                "state": "state-2",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
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
                "code_verifier": PKCE_VERIFIER,
            },
        ).json()
        assert public_client_exchange["access_token"].count(".") == 2


def test_pkce_redirect_and_refresh_replay_are_enforced(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-security.db")
    import asyncio

    asyncio.run(seed_database(database_url))

    def login_code(client: TestClient) -> str:
        response = client.post(
            "/oauth2/doLogin",
            data={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "state": "security-state",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
                "username": "admin",
                "password": "admin123",
            },
        ).json()
        return parse_qs(urlparse(response["result"]).query)["code"][0]

    with TestClient(create_app(auth_config(database_url))) as client:
        invalid_authorize = client.get(
            "/oauth2/authorize",
            params={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "https://attacker.test/callback",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
            },
        )
        assert invalid_authorize.status_code == 400
        assert "redirect_uri未登记" in invalid_authorize.text

        unregistered = client.post(
            "/oauth2/doLogin",
            data={
                "client_id": "JBM",
                "redirect_uri": "https://attacker.test/callback",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
                "username": "admin",
                "password": "admin123",
            },
        ).json()
        assert unregistered["success"] is False
        assert unregistered["message"] == "redirect_uri未登记"

        missing_pkce = client.post(
            "/oauth2/doLogin",
            data={
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "username": "admin",
                "password": "admin123",
            },
        ).json()
        assert missing_pkce["success"] is False

        wrong_verifier = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": login_code(client),
                "code_verifier": "wrong-verifier-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            },
        ).json()
        assert wrong_verifier["error"] == "invalid_grant"

        token_response = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": login_code(client),
                "code_verifier": PKCE_VERIFIER,
            },
        )
        assert token_response.headers["cache-control"] == "no-store"
        first = token_response.json()
        introspected = client.post(
            "/oauth2/introspect",
            data={
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "token": first["access_token"],
                "token_type_hint": "access_token",
            },
        ).json()
        assert introspected["active"] is True
        assert introspected["client_id"] == "JBM"
        rotated = client.post(
            "/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "JBM",
                "refresh_token": first["refresh_token"],
            },
        ).json()
        assert rotated["refresh_token"] != first["refresh_token"]
        assert client.get(
            "/oauth2/userinfo",
            headers={"Authorization": "Bearer " + first["access_token"]},
        ).status_code == 401
        assert client.post(
            "/oauth2/introspect",
            data={
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "token": first["access_token"],
            },
        ).json() == {"active": False}

        replay = client.post(
            "/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "JBM",
                "refresh_token": first["refresh_token"],
            },
        ).json()
        assert replay["error"] == "invalid_grant"
        assert client.get(
            "/oauth2/userinfo",
            headers={"Authorization": "Bearer " + rotated["access_token"]},
        ).status_code == 401
        rotated_reuse = client.post(
            "/oauth2/token",
            data={
                "grant_type": "refresh_token",
                "client_id": "JBM",
                "refresh_token": rotated["refresh_token"],
            },
        ).json()
        assert rotated_reuse["error"] == "invalid_grant"

        revocable = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": login_code(client),
                "code_verifier": PKCE_VERIFIER,
            },
        ).json()
        revoked = client.post(
            "/oauth2/revoke",
            data={
                "client_id": "JBM",
                "token": revocable["refresh_token"],
                "token_type_hint": "refresh_token",
            },
        )
        assert revoked.status_code == 200
        assert client.get(
            "/oauth2/userinfo",
            headers={"Authorization": "Bearer " + revocable["access_token"]},
        ).status_code == 401


def test_all_configured_login_modes_issue_tokens(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-modes.db")
    import asyncio

    asyncio.run(seed_database(database_url))

    with TestClient(create_app(auth_config(database_url))) as client:
        modes = [
            ("PASSWORD", "admin", "admin123"),
            ("SMS", "13800000000", "99999"),
            ("FACE", "13800000000", "data:image/png;base64," + "A" * 64),
            ("WECHAT", "wechat-openid", "wechat-test-code"),
            ("MINIAPP", "13800000000", "miniapp-test-code"),
        ]
        for index, (login_type, username, credential) in enumerate(modes):
            login = client.post(
                "/oauth2/doLogin",
                data={
                    "response_type": "code",
                    "client_id": "JBM",
                    "redirect_uri": "http://admin.test/login/callback",
                    "state": "mode-%s" % index,
                    "code_challenge": PKCE_CHALLENGE,
                    "code_challenge_method": "S256",
                    "username": username,
                    "password": credential,
                    "loginType": login_type,
                },
            ).json()
            assert login["success"] is True, (login_type, login)
            code = parse_qs(urlparse(login["result"]).query)["code"][0]
            token = client.post(
                "/oauth2/token",
                data={
                    "grant_type": "authorization_code",
                    "client_id": "JBM",
                    "redirect_uri": "http://admin.test/login/callback",
                    "code": code,
                    "code_verifier": PKCE_VERIFIER,
                },
            ).json()
        assert token["access_token"].count(".") == 2, login_type

        thirdparty = client.get(
            "/oauth2/thirdparty/mock/callback",
            params={
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "state": "thirdparty-state",
                "code": "thirdparty-test-code",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
            },
        ).json()
        assert thirdparty["success"] is True
        thirdparty_code = parse_qs(urlparse(thirdparty["result"]).query)["code"][0]
        thirdparty_token = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": thirdparty_code,
                "code_verifier": PKCE_VERIFIER,
            },
        ).json()
        assert thirdparty_token["access_token"].count(".") == 2


@pytest.mark.asyncio
async def test_sms_login_binds_existing_profile_instead_of_creating_user() -> None:
    class SmsRegistrationRepository:
        account: dict[str, object] | None = None
        bound_user_id: int | None = None

        async def find_account(self, account: str, account_type: str, domain: str):
            if self.account and self.account["account"] == account:
                return self.account
            return None

        async def find_users_by_mobile(self, mobile: str):
            return [{
                "user_id": 1,
                "user_name": "admin",
                "user_type": "platform",
                "company_id": 1,
                "mobile": mobile,
                "status": 1,
                "close_time": None,
            }]

        async def bind_mobile_account(self, user_id: int, mobile: str, domain: str):
            self.bound_user_id = user_id
            self.account = {
                "account": mobile,
                "account_type": "mobile",
                "user_id": user_id,
                "status": 1,
                "domain": domain,
            }
            return self.account

        async def find_user(self, user_id: int):
            return {
                "user_id": user_id,
                "user_name": "admin",
                "user_type": "platform",
                "company_id": 1,
                "mobile": "13900000001",
                "status": 1,
                "close_time": None,
            }

    repository = SmsRegistrationRepository()
    service = AuthService(
        repository,  # type: ignore[arg-type]
        TokenCache(RedisClient({"enabled": False})),
        {},
    )

    async def verify_phone_code(_phone: str, _code: str) -> bool:
        return True

    service.verify_phone_code = verify_phone_code  # type: ignore[method-assign]
    account, user, scope = await service._authenticate_user_for_client(
        {
            "appId": 1000,
            "registration": {"enabled": True, "mode": "tenant", "defaultRoleCode": "tenant_admin"},
        },
        {"loginType": "SMS", "username": "13900000001", "password": "123456"},
    )

    assert account["account"] == "13900000001"
    assert user["company_id"] == 1
    assert scope == "all"
    assert repository.bound_user_id == 1


@pytest.mark.asyncio
async def test_sms_login_uses_the_same_failure_backoff_as_password_login() -> None:
    service = AuthService(
        object(),  # type: ignore[arg-type]
        TokenCache(RedisClient({"enabled": False})),
        {"login-error-number": 2, "login-error-limit-minutes": 10},
    )

    async def reject_code(_phone: str, _code: str) -> bool:
        raise AuthError("验证码错误", 400)

    service.verify_phone_code = reject_code  # type: ignore[method-assign]
    form = {"loginType": "SMS", "username": "13900000009", "password": "123456"}
    for _ in range(2):
        with pytest.raises(AuthError, match="验证码错误"):
            await service._authenticate_user_for_client({}, form)

    with pytest.raises(AuthError, match="登录失败次数过多") as locked:
        await service._authenticate_user_for_client({}, form)
    assert locked.value.code == 423


@pytest.mark.asyncio
async def test_sms_login_creates_unknown_mobile_only_once() -> None:
    class UnknownMobileRepository:
        account: dict[str, object] | None = None
        created_count = 0

        async def find_account(self, account: str, _account_type: str, _domain: str):
            return self.account if self.account and self.account["account"] == account else None

        async def find_users_by_mobile(self, _mobile: str):
            return []

        async def create_tenant_account(self, **kwargs: object):
            self.created_count += 1
            self.account = {
                "account": kwargs["username"],
                "account_type": "mobile",
                "user_id": 2001,
                "status": 1,
                "domain": kwargs["domain"],
            }
            return {"tenantId": 1001, "userId": 2001}

        async def find_user(self, user_id: int):
            return {
                "user_id": user_id,
                "user_name": "13900000002",
                "user_type": "tenant",
                "company_id": 1001,
                "mobile": "13900000002",
                "status": 1,
                "close_time": None,
            }

    repository = UnknownMobileRepository()
    service = AuthService(
        repository,  # type: ignore[arg-type]
        TokenCache(RedisClient({"enabled": False})),
        {},
    )

    async def verify_phone_code(_phone: str, _code: str) -> bool:
        return True

    service.verify_phone_code = verify_phone_code  # type: ignore[method-assign]
    client = {
        "appId": 1000,
        "registration": {"enabled": True, "mode": "tenant", "defaultRoleCode": "tenant_admin"},
    }
    for _ in range(2):
        account, user, _scope = await service._authenticate_user_for_client(
            client,
            {"loginType": "SMS", "username": "13900000002", "password": "123456"},
        )
        assert account["user_id"] == 2001
        assert user["company_id"] == 1001
    assert repository.created_count == 1


@pytest.mark.asyncio
async def test_repository_binds_profile_mobile_to_existing_user(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "mobile-binding.db")
    await seed_database(database_url)
    engine = create_async_engine(database_url)
    async with engine.begin() as conn:
        await conn.execute(
            text("UPDATE base_user SET mobile='13585658904' WHERE user_name='admin'")
        )
    await engine.dispose()

    repository = AuthRepository({"url": database_url})
    await repository.start()
    try:
        users = await repository.find_users_by_mobile("13585658904")
        assert [user["user_name"] for user in users] == ["admin"]
        user_id = int(users[0]["user_id"])
        await repository.bind_mobile_account(user_id, "13585658904", "@admin.com")
        account = await repository.find_account("13585658904", "mobile", "@admin.com")
        assert account and account["user_id"] == user_id
    finally:
        await repository.stop()


def test_configured_provider_adapter_verifies_and_binds_subject(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-provider.db")
    import asyncio

    asyncio.run(seed_database(database_url))
    app = create_app(auth_config(database_url))

    class ProviderClient:
        async def post(self, url: str, **kwargs: object) -> httpx.Response:
            assert url == "https://identity.test/verify"
            assert kwargs["json"] == {
                "provider": "wechat",
                "loginType": "WECHAT",
                "subjectHint": "wechat-openid",
                "credential": "provider-one-time-code",
            }
            return httpx.Response(200, json={"verified": True, "subject": "wechat-openid"})

    with TestClient(app) as client:
        service = app.state.auth_service
        service.dev_bypass_enabled = False
        service.login_providers["wechat"] = {
            "enabled": True,
            "verify-url": "https://identity.test/verify",
            "account-type": "wechat",
        }
        service.http_client = ProviderClient()
        login = client.post(
            "/oauth2/doLogin",
            data={
                "response_type": "code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
                "username": "wechat-openid",
                "password": "provider-one-time-code",
                "loginType": "WECHAT",
            },
        ).json()
        code = parse_qs(urlparse(login["result"]).query)["code"][0]
        token = client.post(
            "/oauth2/token",
            data={
                "grant_type": "authorization_code",
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code": code,
                "code_verifier": PKCE_VERIFIER,
            },
        ).json()
        assert token["access_token"].count(".") == 2


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
            assert failed["error"] == "invalid_grant"
            assert failed["error_description"] == "用户名或密码错误"

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
        assert locked["error"] == "invalid_grant"
        assert locked["error_description"] == "登录失败次数过多，帐户锁定10分钟"

    async def audit_failures() -> int:
        engine = create_async_engine(database_url)
        async with engine.connect() as conn:
            count = await conn.scalar(text("SELECT COUNT(*) FROM base_account_logs WHERE status = 0"))
        await engine.dispose()
        return int(count or 0)

    assert asyncio.run(audit_failures()) == 6


def test_register_captcha_and_qrcode_frontend_paths(tmp_path: Path) -> None:
    database_url = "sqlite+aiosqlite:///%s" % (tmp_path / "auth-register.db")
    import asyncio

    public_key, private_key = rsa_pair_base64()
    asyncio.run(seed_database(database_url, public_key, private_key))
    loaded_public = serialization.load_der_public_key(base64.b64decode(public_key))
    encrypted_password = base64.b64encode(loaded_public.encrypt(b"NewPass@123", padding.PKCS1v15())).decode("ascii")

    with TestClient(create_app(auth_config(database_url))) as client:
        captcha = client.get("/captcha/vcode64", params={"width": 120, "height": 40}).json()
        assert captcha["success"] is True
        assert captcha["result"].startswith(("data:image/png;base64,", "data:image/svg+xml;base64,"))

        registered = client.post(
            "/oauth2/register",
            headers={"X-Password-Encrypted": "true"},
            data={
                "client_id": "JBM",
                "client_secret": "demo-secret",
                "tenantName": "New User Tenant",
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
                "password": "NewPass@123",
                "vcode": "9999",
            },
        ).json()
        assert logged_in["access_token"].count(".") == 2

        qr = client.post(
            "/qrcode/login",
            params={
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
                "width": 180,
                "height": 180,
            },
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
        access_token = token_response["access_token"]

        qr = client.post(
            "/qrcode/login",
            params={
                "client_id": "JBM",
                "redirect_uri": "http://admin.test/login/callback",
                "code_challenge": PKCE_CHALLENGE,
                "code_challenge_method": "S256",
            },
        ).json()["result"]
        code = qr["code"]

        scanned = client.post("/qrcode/scanned", params={"code": code}).json()
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
                "code_verifier": PKCE_VERIFIER,
            },
        ).json()
        assert exchanged["access_token"].count(".") == 2
        assert exchanged["login_id"] == "normal:1000:2057849052900044802"


def test_phone_code_is_sent_through_push_notification() -> None:
    import asyncio

    requests: list[dict[str, object]] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(dict(request.url.params))
        return httpx.Response(200, json={"success": True, "result": {"Code": "OK", "pin": "123456"}})

    async def run() -> bool:
        client = httpx.AsyncClient(transport=httpx.MockTransport(handler), base_url="http://push.test")
        service = AuthService(
            repository=None,  # type: ignore[arg-type]
            cache=TokenCache(RedisClient({"enabled": False})),
            config={"dev-bypass-enabled": True, "sms": {"push-base-url": "http://push.test"}},
            http_client=client,
        )
        try:
            return await service.send_phone_code("13585658904", "9999")
        finally:
            await client.aclose()

    assert asyncio.run(run()) is True
    assert requests[0]["phoneNumber"] == "13585658904"
    assert set(requests[0]) == {"phoneNumber"}


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
                    "pin": "99999",
                },
            },
        )

    async def run() -> bool:
        client = httpx.AsyncClient(transport=httpx.MockTransport(handler), base_url="http://push.test")
        service = AuthService(
            repository=None,  # type: ignore[arg-type]
            cache=TokenCache(RedisClient({"enabled": False})),
            config={"dev-bypass-enabled": True, "sms": {"push-base-url": "http://push.test"}},
            http_client=client,
        )
        try:
            return await service.send_phone_code("13585658904", "9999")
        finally:
            await client.aclose()

    assert asyncio.run(run()) is True


def test_dev_sms_bypass_still_validates_phone_number() -> None:
    import asyncio

    service = AuthService(
        repository=None,  # type: ignore[arg-type]
        cache=TokenCache(RedisClient({"enabled": False})),
        config={"dev-bypass-enabled": True},
    )

    with pytest.raises(AuthError, match="非法手机号"):
        asyncio.run(service.verify_phone_code("", "99999"))


def test_dev_sms_bypass_accepts_99999_without_calling_push() -> None:
    import asyncio

    service = AuthService(
        repository=None,  # type: ignore[arg-type]
        cache=TokenCache(RedisClient({"enabled": False})),
        config={"dev-bypass-enabled": True, "sms": {"push-base-url": "http://push.test"}},
    )

    assert asyncio.run(service.verify_phone_code("13585658904", "99999")) is True


def test_phone_code_routes_send_and_check_through_push() -> None:
    import asyncio

    requests: list[tuple[str, dict[str, str]]] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append((request.url.path, dict(request.url.params)))
        if request.url.path == "/pin/send":
            return httpx.Response(200, json={"success": True, "result": {"Code": "OK"}})
        return httpx.Response(
            200,
            json={"success": True, "result": {"Code": "OK", "VerifyResult": "PASS"}},
        )

    async def run() -> None:
        client = httpx.AsyncClient(transport=httpx.MockTransport(handler), base_url="http://push.test")
        service = AuthService(
            repository=None,  # type: ignore[arg-type]
            cache=TokenCache(RedisClient({"enabled": False})),
            config={"dev-bypass-enabled": True, "sms": {"push-base-url": "http://push.test"}},
            http_client=client,
        )
        try:
            assert await service.send_phone_code("13585658904", "9999") is True
            assert await service.verify_phone_code("13585658904", "123456") is True
        finally:
            await client.aclose()

    asyncio.run(run())
    assert requests == [
        ("/pin/send", {"phoneNumber": "13585658904"}),
        ("/pin/check", {"phoneNumber": "13585658904", "code": "123456"}),
    ]


@pytest.mark.asyncio
async def test_current_user_binds_mobile_and_email_only_after_verification() -> None:
    class Repository:
        mobile = ""
        email = ""

        async def find_account(self, _account: str, _account_type: str, _domain: str):
            return None

        async def find_users_by_mobile(self, _mobile: str):
            return []

        async def find_users_by_email(self, _email: str):
            return []

        async def password_accounts_for_user(self, user_id: int):
            return [{"user_id": user_id, "password": "$2b$existing"}]

        async def bind_mobile_account(
            self, user_id: int, mobile: str, domain: str, password_hash: str
        ):
            assert (user_id, domain, password_hash) == (1, "@admin.com", "$2b$existing")
            self.mobile = mobile
            return {"account": mobile}

        async def bind_email_account(
            self, user_id: int, email: str, domain: str, password_hash: str
        ):
            assert (user_id, domain, password_hash) == (1, "@admin.com", "$2b$existing")
            self.email = email
            return {"account": email}

    repository = Repository()
    service = AuthService(
        repository,  # type: ignore[arg-type]
        TokenCache(RedisClient({"enabled": False})),
        {"dev-bypass-enabled": True},
    )

    async def userinfo(_token: str):
        return {"userId": 1}

    service.userinfo = userinfo  # type: ignore[method-assign]
    await service.send_mobile_bind_code("token", "13800000019")
    await service.send_email_bind_code("token", "User@Example.com")
    assert repository.mobile == ""
    assert repository.email == ""
    await service.bind_mobile("token", "13800000019", "99999")
    await service.bind_email("token", "User@Example.com", "99999")
    assert repository.mobile == "13800000019"
    assert repository.email == "user@example.com"
