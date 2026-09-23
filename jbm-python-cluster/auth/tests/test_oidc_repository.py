from __future__ import annotations

import asyncio
import importlib.util
import json
from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest
from alembic.migration import MigrationContext
from alembic.operations import Operations
from jbm_cluster_py.platform.auth.repository import AuthRepository, _client_oidc_settings
from sqlalchemy import create_engine, inspect, text


def registration(**changes):
    return {
        "enabled": True,
        "redirectUris": ["https://rp.example/callback"],
        "postLogoutRedirectUris": ["https://rp.example/signed-out"],
        "scopes": ["openid", "profile", "email", "offline_access"],
        "audiences": ["https://api.example"],
        "tokenEndpointAuthMethod": "none",
        **changes,
    }


def test_oidc_registration_requires_explicit_opt_in_without_legacy_fallback():
    for value in (None, {}, {"enabled": "true"}, {"enabled": 1}, {"enabled": False}):
        assert _client_oidc_settings(
            {
                "website": "https://rp.example/callback",
                "extend_data": {"oidc": value, "oauth": {"publicClient": True}},
            }
        ) == {"enabled": False}


@pytest.mark.parametrize(
    "changes",
    [
        {"redirectUris": []},
        {"redirectUris": "https://rp.example/callback"},
        {"redirectUris": ["https://rp.example/*"]},
        {"redirectUris": ["https://rp.example/callback#fragment"]},
        {"redirectUris": ["https://name:password@rp.example/callback"]},
        {"redirectUris": ["http://rp.example/callback"]},
        {"redirectUris": ["/callback"]},
        {"redirectUris": ["https://rp.example:bad/callback"]},
        {"postLogoutRedirectUris": ["//evil.example"]},
        {"scopes": ["profile"]},
        {"scopes": ["openid", "all"]},
        {"scopes": ["openid", 1]},
        {"audiences": []},
        {"tokenEndpointAuthMethod": "client_secret_basic"},
        {"tokenEndpointAuthMethod": "unsupported"},
        {"grantTypes": ["authorization_code", "password"]},
        {"grantTypes": ["authorization_code"]},
        {"trusted": "true"},
    ],
)
def test_oidc_registration_fails_closed(changes):
    settings = _client_oidc_settings({"extend_data": {"oidc": registration(**changes)}})
    assert settings == {"enabled": False, "configurationError": "Invalid OIDC client configuration"}


def test_oidc_registration_normalizes_without_exposing_secrets():
    settings = _client_oidc_settings(
        {
            "secret_key": "private-client-secret",
            "extend_data": json.dumps(
                {
                    "oidc": registration(
                        redirectUris=["http://127.0.0.1:18123/callback"],
                        tokenEndpointAuthMethod="client_secret_basic",
                        scopes=["openid", "profile", "openid"],
                    )
                }
            ),
        }
    )
    assert settings["enabled"] is True
    assert settings["scopes"] == ["openid", "profile"]
    assert settings["grantTypes"] == ["authorization_code", "refresh_token"]
    assert settings["trusted"] is False
    assert "private-client-secret" not in repr(settings)


@pytest.mark.asyncio
async def test_subject_converges_across_instances_and_survives_restart(tmp_path):
    config = {"url": f"sqlite+aiosqlite:///{tmp_path / 'subjects.db'}"}
    first, second = AuthRepository(config), AuthRepository(config)
    await first.start()
    async with first.engine.begin() as conn:
        await conn.execute(
            text("INSERT INTO base_user (user_id, user_name) VALUES (100, 'alice'), (200, 'bob')")
        )
    subjects = await asyncio.gather(
        *((first if i % 2 else second).oidc_subject(100) for i in range(20))
    )
    assert len(set(subjects)) == 1
    assert len(subjects[0]) == 43
    assert await second.oidc_subject(200) != subjects[0]
    async with first.engine.begin() as conn:
        await conn.execute(
            text("UPDATE base_user SET user_name='renamed', company_id=42 WHERE user_id=100")
        )
    await first.stop()
    await second.stop()
    reopened = AuthRepository(config)
    assert await reopened.oidc_subject(100) == subjects[0]
    with pytest.raises(ValueError, match="unknown user"):
        await reopened.oidc_subject(999)
    await reopened.stop()


@pytest.mark.asyncio
async def test_subject_collision_retries_without_reassigning_existing_subject(
    tmp_path, monkeypatch
):
    repo = AuthRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'collision.db'}"})
    await repo.start()
    async with repo.engine.begin() as conn:
        await conn.execute(text("INSERT INTO base_user (user_id) VALUES (100), (200)"))
    original = await repo.oidc_subject(100)
    values = iter([original, "fresh-opaque-subject"])
    monkeypatch.setattr(
        "jbm_cluster_py.platform.auth.repository.secrets.token_urlsafe", lambda _: next(values)
    )
    assert await repo.oidc_subject(200) == "fresh-opaque-subject"
    assert await repo.oidc_subject(100) == original
    await repo.stop()


@pytest.mark.asyncio
async def test_missing_oidc_migration_has_actionable_error(tmp_path):
    repo = AuthRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'missing.db'}"})
    with pytest.raises(RuntimeError, match="migration required.*base_auth_subject"):
        await repo.require_oidc_schema()
    await repo.stop()


@pytest.mark.asyncio
async def test_find_client_preserves_legacy_contract_and_rejects_expired_keys(tmp_path):
    repo = AuthRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'clients.db'}"})
    await repo.start()
    async with repo.engine.begin() as conn:
        await conn.execute(
            text(
                "INSERT INTO base_app "
                "(app_id, api_key, status, website, extend_data) VALUES "
                "(1, 'legacy', 1, 'https://legacy.example', :data)"
            ),
            {"data": json.dumps({"oidc": registration()})},
        )
        values = [
            None,
            "2000-01-01",
            "not-a-date",
            (datetime.now(UTC) + timedelta(days=1)).isoformat(),
        ]
        for index, expiry in enumerate(values):
            await conn.execute(
                text(
                    "INSERT INTO base_api_key "
                    "(key_id, api_key, status, expire_time) VALUES (:id, :key, 1, :expiry)"
                ),
                {"id": index, "key": f"key{index}", "expiry": expiry},
            )
    client = await repo.find_client("legacy")
    assert client["redirectUris"] == ["https://legacy.example"]
    assert client["publicClient"] is False
    assert client["oidc"]["enabled"] is True
    assert (await repo.find_client("key0"))["oidc"] == {"enabled": False}
    assert await repo.find_client("key1") is None
    assert await repo.find_client("key2") is None
    assert await repo.find_client("key3") is not None
    await repo.stop()


@pytest.mark.asyncio
async def test_oidc_context_requires_current_explicit_membership_and_app_grants(tmp_path):
    repo = AuthRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'context.db'}"})
    await repo.start()
    async with repo.engine.begin() as conn:
        await conn.execute(
            text(
                "INSERT INTO base_app (app_id, api_key, status, extend_data) "
                "VALUES (1, 'rp', 1, :data)"
            ),
            {"data": json.dumps({"oidc": registration()})},
        )
        await conn.execute(
            text(
                "INSERT INTO base_user "
                "(user_id, user_name, company_id, status) VALUES (7, 'admin', 10, 1)"
            )
        )
        await conn.execute(text("INSERT INTO base_org (id, status) VALUES (10, 1)"))
        await conn.execute(
            text("INSERT INTO base_role (role_id, role_code, status) VALUES (1, 'reader', 1)")
        )
        await conn.execute(
            text("INSERT INTO base_tenant_app (tenant_id, app_id, status) VALUES (10, 1, 1)")
        )
        await conn.execute(
            text(
                "INSERT INTO base_role_user "
                "(user_id, role_id, app_id, tenant_id) VALUES (7, 1, 1, 10)"
            )
        )
    client = await repo.find_client("rp")
    assert await repo.oidc_user_context(7, client) is None
    async with repo.engine.begin() as conn:
        await conn.execute(text("INSERT INTO base_user_org (user_id, org_id) VALUES (7, 10)"))
    context = await repo.oidc_user_context(7, client)
    assert context["tenantId"] == 10
    assert context["roles"] == ["reader"]
    async with repo.engine.begin() as conn:
        await conn.execute(text("UPDATE base_tenant_app SET status=0"))
    assert await repo.oidc_user_context(7, client) is None
    async with repo.engine.begin() as conn:
        await conn.execute(text("UPDATE base_tenant_app SET status=1"))
        await conn.execute(text("UPDATE base_app SET status=0"))
    assert await repo.oidc_user_context(7, client) is None
    await repo.stop()


def test_subject_migration_preserves_mapping_on_downgrade(tmp_path):
    path = Path(__file__).parents[2] / "center/migrations/versions/20260922_23_auth_oidc_subject.py"
    spec = importlib.util.spec_from_file_location("oidc_subject_migration", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    engine = create_engine(f"sqlite:///{tmp_path / 'migration.db'}")
    with engine.begin() as conn:
        with Operations.context(MigrationContext.configure(conn)):
            module.upgrade()
            module.upgrade()
            conn.execute(
                text("INSERT INTO base_auth_subject VALUES (1, 'permanent', '2026-09-22')")
            )
            module.downgrade()
        assert inspect(conn).has_table("base_auth_subject")
        assert conn.execute(text("SELECT subject FROM base_auth_subject")).scalar() == "permanent"
    engine.dispose()
