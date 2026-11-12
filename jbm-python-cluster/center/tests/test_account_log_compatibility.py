"""Legacy login audit routes use the logs service even without a SQL table."""

from unittest.mock import AsyncMock, Mock

import pytest
from fastapi import HTTPException

from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService
from jbm_cluster_py.platform.center.modules.governance.infrastructure.crud_store import _row


def test_mysql_bit_statuses_are_numeric():
    assert _row({'login_status': b'\x00'}) == {'loginStatus': 0}
    assert _row({'login_status': b'\x01'}) == {'loginStatus': 1}
    assert _row({'status': bytearray(b'\x01')}) == {'status': 1}


@pytest.mark.asyncio
async def test_account_log_compatibility_forwards_filters_and_requires_logs_service():
    store = Mock()
    openapi = Mock()
    openapi.discovery.choose_instance = AsyncMock(return_value={"ip": "logs", "port": 9000})
    upstream = {"success": True, "result": {"total": 1, "contents": [{"loginStatus": False}]}}
    openapi.client.post = AsyncMock(return_value=Mock(status_code=200, json=Mock(return_value=upstream)))
    service = CompatibilityService(store, Mock(), openapi)
    body = {"baseAccountLogs": {"accountType": "REFRESH_FAILED", "loginStatus": False}}
    route = "/baseAccountLogs/pageList"
    assert await service._master_data("POST", route, "accountLogs", "baseAccountLogs", body) == upstream
    assert openapi.client.post.call_args.args == ("http://logs:9000/loginLogs/pageList",)
    assert openapi.client.post.call_args.kwargs["json"] == body
    openapi.discovery.choose_instance.return_value = None
    with pytest.raises(HTTPException) as failure:
        await service._master_data("POST", route, "accountLogs", "baseAccountLogs", body)
    assert failure.value.status_code == 503
    store.list.assert_not_called()
