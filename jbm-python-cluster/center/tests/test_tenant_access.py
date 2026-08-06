from unittest.mock import AsyncMock

import pytest
from fastapi import HTTPException

from jbm_cluster_py.platform.center.modules.governance.application.service import GovernanceService
from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import (
    CompatibilityService,
)


TENANT = {"userId": 2002, "tenantId": 2000, "roles": ["tenant_admin"]}
OPERATOR = {"userId": 2001, "tenantId": 1, "roles": ["platform_operator"]}


@pytest.mark.asyncio
async def test_tenant_filters_override_client_values() -> None:
    repository = AsyncMock()
    repository.list_users.return_value = ([{"userId": 2002, "companyId": 2000}], 1)
    repository.list_apps.return_value = ([{"appId": 2000, "orgId": 2000}], 1)
    service = GovernanceService(repository)

    await service.users(1, 10, None, {"companyId": 1}, TENANT)
    await service.apps(1, 10, {"orgId": 1}, TENANT)

    assert repository.list_users.await_args.args[3]["companyId"] == 2000
    assert repository.list_apps.await_args.args[2]["orgId"] == 2000


@pytest.mark.asyncio
async def test_tenant_cannot_read_cross_tenant_user() -> None:
    repository = AsyncMock()
    repository.get_user.return_value = {"userId": 1, "companyId": 1}
    service = GovernanceService(repository)

    with pytest.raises(HTTPException) as error:
        await service.user(1, TENANT)
    assert error.value.status_code == 403

    assert await service.user(1, OPERATOR) == {"userId": 1, "companyId": 1}


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("method", "path"),
    [
        ("POST", "/baseAccountLogs/list"),
        ("POST", "/dataSourceManagement/saveData"),
        ("POST", "/api-docs/test"),
        ("GET", "/internal/gateway/apikey"),
    ],
)
async def test_tenant_cannot_use_platform_or_internal_compatibility_paths(
    method: str, path: str
) -> None:
    service = CompatibilityService(AsyncMock(), AsyncMock(), AsyncMock())

    with pytest.raises(HTTPException) as error:
        await service.handle(method, path, {}, {}, {}, TENANT)

    assert error.value.status_code == 403
