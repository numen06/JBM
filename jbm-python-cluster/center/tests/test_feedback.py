from types import SimpleNamespace

import httpx
import pytest
from fastapi import FastAPI, HTTPException
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.common.api_access import authorize_api
from jbm_cluster_py.platform.center.feedback import build_feedback_router, metadata


@pytest.mark.asyncio
@pytest.mark.parametrize("mode", ["local", "cloud"])
async def test_feedback_privacy_identity_and_handling(mode):
    engine = create_async_engine("sqlite+aiosqlite://")
    async with engine.begin() as conn:
        await conn.run_sync(metadata.create_all)
    app = FastAPI()
    ordinary = {"userId": 20, "tenantId": 200, "appId": 30, "username": "ordinary", "roles": []}
    tenant_admin = {**ordinary, "userId": 21, "roles": ["tenant_admin"]}
    operator = {"userId": 40, "tenantId": 1, "appId": 30, "roles": ["platform_operator"]}
    current = SimpleNamespace(identity=ordinary)

    @app.middleware("http")
    async def identity(request, call_next):
        request.state.identity = current.identity
        return await call_next(request)

    app.include_router(build_feedback_router(engine, {"deployment-mode": mode}))
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        assert (await client.get("/feedback/context")).json()["result"] == {"deploymentMode": mode, "canManage": False}
        body = {"title": " 改进建议 ", "description": "详细内容", "sourcePage": "/dashboard?token=private"}
        for extra in [{"userId": 1}, {"tenantId": 1}, {"deploymentMode": "cloud"}, {"status": "resolved"}]:
            assert (await client.post("/feedback/submit", json={**body, **extra})).status_code == 422
        assert (await client.post("/feedback/submit", json={**body, "title": "  "})).status_code == 422
        assert (await client.post("/feedback/submit", json={**body, "sourcePage": "//foreign/path"})).status_code == 422
        receipt = (await client.post("/feedback/submit", json=body)).json()["result"]
        assert set(receipt) == {"id"}
        key = receipt["id"]
        for actor in [ordinary, tenant_admin, {**tenant_admin, "roles": [], "permissions": ["all", "MENU_feedback"]}]:
            current.identity = actor
            for url in ["/feedback", f"/feedback/{key}", "/feedback/nonexistent"]:
                assert (await client.get(url)).status_code == 403
            assert (await client.put(f"/feedback/{key}", json={"status": "resolved", "revision": 1})).status_code == 403
            authorize_api("jbm-cluster-platform-center", "POST", "/feedback/submit", actor)
            authorize_api("jbm-cluster-platform-center", "GET", "/feedback/context", actor)
            with pytest.raises(HTTPException) as denied:
                authorize_api("jbm-cluster-platform-center", "GET", "/feedback", actor)
            assert denied.value.status_code == 403
        for actor in [operator, {**operator, "admin": True, "roles": ["super_admin"]}]:
            current.identity = actor
            assert (await client.get("/feedback/context")).json()["result"]["canManage"] is True
            listing = (await client.get("/feedback")).json()["result"]
            assert listing["total"] == 1
            row = (await client.get(f"/feedback/{key}")).json()["result"]
            assert (row["userId"], row["tenantId"], row["appId"], row["deploymentMode"], row["sourcePage"]) == ("20", "200", "30", mode, "/dashboard")
        current.identity = operator
        assert (await client.put(f"/feedback/{key}", json={"status": "processing", "handleNote": "已受理", "revision": 1})).status_code == 200
        assert (await client.put(f"/feedback/{key}", json={"status": "resolved", "revision": 1})).status_code == 409
        assert (await client.put(f"/feedback/{key}", json={"status": "resolved", "handleNote": "已完成", "revision": 2})).status_code == 200
        row = (await client.get("/feedback", params={"status": "resolved"})).json()["result"]["items"][0]
        assert row["status"] == "resolved" and row["handleNote"] == "已完成" and row["handledBy"] == "40"
        assert (await client.get("/feedback", params={"page": 0})).status_code == 422
        current.identity = {"clientId": "service", "scope": "internal"}
        assert (await client.post("/feedback/submit", json=body)).status_code == 403
    await engine.dispose()


@pytest.mark.asyncio
async def test_new_test_account_password_stays_out_of_user_profile():
    from unittest.mock import AsyncMock
    from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService
    store = AsyncMock()
    store.find_user_by_username.return_value = None
    store.save.return_value = {"userId": 50, "companyId": 1, "userName": "jbm_operator"}
    service = CompatibilityService(store, AsyncMock(), AsyncMock())
    await service._create_user({"userName": "jbm_operator", "companyId": 1, "password": "Local-Test!92846"}, {"userId": 1, "tenantId": 1, "admin": True})
    calls = store.save.await_args_list
    assert calls[0].args[0] == "user" and "password" not in calls[0].args[1]
    assert calls[1].args[0] == "account" and calls[1].args[1]["password"].startswith("$2")


@pytest.mark.parametrize("existing", [None, "platform_operator", "custom_business_role"])
def test_migration_installs_standard_role_without_overwriting_existing_data(existing, monkeypatch):
    import importlib.util
    from pathlib import Path
    import sqlalchemy as sa

    path = Path(__file__).parents[1]/"migrations/versions/20261104_29_feedback.py"
    spec = importlib.util.spec_from_file_location("feedback_migration", path)
    migration = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(migration)
    engine = sa.create_engine("sqlite://")
    with engine.begin() as conn:
        conn.execute(sa.text("CREATE TABLE base_role (role_id BIGINT PRIMARY KEY, role_code VARCHAR(100), role_name VARCHAR(100), role_desc VARCHAR(100), status INT, is_persist INT, create_time DATETIME, update_time DATETIME)"))
        if existing:
            conn.execute(sa.text("INSERT INTO base_role (role_id,role_code,role_name,status) VALUES (3,:code,'已有名称',0)"), {"code":existing})
        monkeypatch.setattr(migration.op, "get_bind", lambda: conn)
        if existing == "custom_business_role":
            with pytest.raises(ValueError, match="已被占用"):
                migration.upgrade()
        else:
            migration.upgrade()
            migration.upgrade()
        rows = conn.execute(sa.text("SELECT role_id,role_code,role_name,status FROM base_role")).all()
        expected = (3, existing, "已有名称", 0) if existing else (3, "platform_operator", "平台运营", 1)
        assert rows == [expected]
    engine.dispose()
