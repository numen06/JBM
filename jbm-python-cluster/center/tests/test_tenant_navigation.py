from unittest.mock import AsyncMock

import pytest
from fastapi import HTTPException
from jbm_cluster_py.platform.center.modules.governance.application.navigation import validate_navigation
from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService


@pytest.mark.asyncio
async def test_navigation_uses_active_tenant_and_only_owner_admin_can_save():
    repo = AsyncMock()
    repo.get_navigation.return_value = {"preferences": {}, "revision": 0}
    repo.get_user.return_value = {"companyId": 20}
    api = GovernanceService(repo)
    owner = {"userId": 1, "tenantId": 20, "roles": ["tenant_admin"]}
    assert (await api.navigation(owner))["canManage"]
    await api.save_navigation(owner, {"revision": 0, "preferences": {"workspaces": {"iot": {"label": "设备"}}}, "tenantId": 30})
    assert repo.save_navigation.await_args.args[0] == 20
    assert repo.save_navigation.await_args.args[1]["workspaces"]["iot"]["label"] == "设备"
    for identity in [dict(owner, roles=["user"]), dict(owner, tenantId=30)]:
        with pytest.raises(HTTPException) as error:
            await api.save_navigation(identity, {"revision": 0, "preferences": {}})
        assert error.value.status_code == 403
    assert repo.save_navigation.await_count == 1
    assert (await api.navigation(dict(owner, tenantId=30)))["tenantId"] == "30"
    repo.get_navigation.assert_awaited_with(30)


@pytest.mark.parametrize("value", [
    {"menus": {"https://other.test": {}}}, {"workspaces": {"extra": {}}},
    {"menus": {"/dashboard": {"permission": "super_admin"}}},
    {"menus": {"/dashboard": {"label": "x" * 41}}},
    {"menus": {"/dashboard": {"hidden": "false"}}},
    {"groups": {"iot:test": {"order": -1}}},
])
def test_navigation_rejects_routes_permission_fields_and_invalid_values(value):
    with pytest.raises(HTTPException):
        validate_navigation(value)


@pytest.mark.asyncio
async def test_repository_isolation_and_optimistic_revision():
    from sqlalchemy import text
    from sqlalchemy.ext.asyncio import create_async_engine
    from jbm_cluster_py.platform.center.modules.governance.infrastructure.repository import SqlGovernanceRepository
    repo = object.__new__(SqlGovernanceRepository)
    repo.engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    try:
        async with repo.engine.begin() as connection:
            await connection.execute(text("CREATE TABLE base_tenant_navigation (tenant_id BIGINT PRIMARY KEY, preferences JSON NOT NULL, revision INTEGER NOT NULL, updated_by BIGINT)"))
        preferences = validate_navigation({"workspaces": {"iot": {"label": "物联设备"}}})
        await repo.save_navigation(20, preferences, 0, 1)
        assert (await repo.get_navigation(20))["preferences"] == preferences
        assert (await repo.get_navigation(30))["revision"] == 0
        for stale in [0, 2]:
            with pytest.raises(HTTPException) as error:
                await repo.save_navigation(20, {}, stale, 2)
            assert error.value.status_code == 409
        await repo.save_navigation(20, validate_navigation({}), 1, 1)
        assert (await repo.get_navigation(20))["revision"] == 2
        assert (await repo.get_navigation(30))["revision"] == 0
    finally:
        await repo.engine.dispose()


@pytest.mark.asyncio
async def test_selected_tenant_is_validated_and_delegation_never_allows_menu_writes():
    repo = AsyncMock()
    repo.get_navigation.return_value = {"preferences": {}, "revision": 0}
    repo.get_user.return_value = {"companyId": 20}
    repo.has_navigation_delegation.return_value = False
    api = GovernanceService(repo)
    identity = {"userId": 1, "tenantId": 20, "appId": 100, "roles": ["tenant_admin"]}
    for method in [lambda: api.navigation(identity, "30"), lambda: api.save_navigation(identity, {"revision": 0, "preferences": {}}, "30")]:
        with pytest.raises(HTTPException) as error:
            await method()
        assert error.value.status_code == 403
    repo.has_navigation_delegation.return_value = True
    assert not (await api.navigation(identity, "30"))["canManage"]
    with pytest.raises(HTTPException) as error:
        await api.save_navigation(identity, {"revision": 0, "preferences": {}}, "30")
    assert error.value.status_code == 403
    repo.navigation_tenant_exists.return_value = True
    root = dict(identity, roles=["super_admin"])
    assert (await api.navigation(root, "30"))["tenantId"] == "30"
    await api.save_navigation(root, {"revision": 0, "preferences": {}}, "30")
    assert repo.save_navigation.await_args.args[0] == 30
