from types import SimpleNamespace
from unittest.mock import AsyncMock

from sqlalchemy import text
from test_key_login import client


def test_log_service_mode_keeps_only_current_state_and_pending_alerts_in_mysql(client):
    repo = client.app.state.repository
    audit = SimpleNamespace(enabled=True, record=AsyncMock())
    repo.security_audit = audit
    async def run():
        async with repo.engine.connect() as conn:
            before = (await conn.execute(text("SELECT COUNT(*) FROM base_account_logs"))).scalar()
        await repo.record_login(user_id=123, account="test", login_type="PASSWORD", ip="198.51.100.3", user_agent="test", details={"risk_level": "warning", "risk_detail": '{"reason":"LOCATION_CHANGED"}'})
        assert await repo.recent_successful_logins(123)
        alerts = await repo.login_alerts(123)
        assert len(alerts) == 1
        assert await repo.login_alerts(999) == []
        ids = [int(alerts[0]["id"])]
        await repo.acknowledge_login_alerts(999, ids)
        assert len(await repo.login_alerts(123)) == 1
        await repo.acknowledge_login_alerts(123, ids)
        assert await repo.login_alerts(123) == []
        async with repo.engine.connect() as conn:
            assert (await conn.execute(text("SELECT COUNT(*) FROM base_account_logs"))).scalar() == before
    client.portal.call(run)
    event = audit.record.call_args_list[0].args[0]
    assert event["category"] == "login" and event["userId"] == 123
    assert event["riskLevel"] == "warning"


def test_password_age_grace_does_not_change_password_or_reset_on_profile_edit(client):
    repo = client.app.state.repository
    async def run():
        account = await repo.find_account("admin", "username", "@admin.com")
        original = dict(account)
        assert await repo.password_age(account, 10000000) == 0
        assert await repo.password_age({**account, "update_time": "profile-only-change"}, 10000000 + 90*86400) == 90*86400
        after = await repo.find_account("admin", "username", "@admin.com")
        assert after["password"] == original["password"]
        assert after["must_change_password"] == original["must_change_password"]
    client.portal.call(run)
