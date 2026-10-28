"""Both deployed account-log schemas expose events and preserve failed results."""
from unittest.mock import AsyncMock

import pytest
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.platform.center.modules.governance.application.compatibility_service import CompatibilityService
from jbm_cluster_py.platform.center.modules.governance.infrastructure.crud_store import CrudStore, _row


def test_mysql_bit_statuses_are_numeric():
    assert _row({'login_status': b'\x00'}) == {'loginStatus': 0}
    assert _row({'login_status': b'\x01'}) == {'loginStatus': 1}
    assert _row({'status': bytearray(b'\x01')}) == {'status': 1}


@pytest.mark.asyncio
@pytest.mark.parametrize("legacy", [False, True])
async def test_log_aliases_filters_and_latest_first(tmp_path, legacy):
    engine = create_async_engine(f"sqlite+aiosqlite:///{tmp_path / 'logs.db'}")
    kind, ip, status, agent = ("account_type", "login_ip", "login_status", "login_agent") if legacy else (
        "login_type", "ip", "status", "user_agent")
    async with engine.begin() as conn:
        await conn.execute(text(f"CREATE TABLE base_account_logs (id INTEGER PRIMARY KEY, login_time TEXT, {kind} TEXT, {ip} TEXT, {status} INTEGER, {agent} TEXT)"))
        await conn.execute(text(f"INSERT INTO base_account_logs VALUES (1, '2026-10-28', 'LOGIN:PASSWORD', '127.0.0.1', 1, 'old'), (2, '2026-10-29', 'REFRESH_FAILED', '127.0.0.2', 0, 'new')"))
    service = CompatibilityService(CrudStore(engine), AsyncMock(), AsyncMock())
    result = await service._master_data('POST', '/baseAccountLogs/pageList', 'accountLogs', 'baseAccountLogs', {})
    rows = result['result']['contents']
    assert [row['id'] for row in rows] == [2, 1]
    assert rows[0]['loginType'] == rows[0]['accountType'] == 'REFRESH_FAILED'
    assert rows[0]['loginStatus'] is False
    assert rows[0]['loginIp'] == '127.0.0.2'
    assert rows[0]['userAgent'] == rows[0]['loginAgent'] == 'new'
    filtered = await service._master_data('POST', '/baseAccountLogs/pageList', 'accountLogs', 'baseAccountLogs', {
        'baseAccountLogs': {'accountType': 'REFRESH_FAILED', 'loginIp': '127.0.0.2', 'loginStatus': False}})
    assert filtered['result']['total'] == 1
    assert filtered['result']['contents'][0]['id'] == 2
    await engine.dispose()
