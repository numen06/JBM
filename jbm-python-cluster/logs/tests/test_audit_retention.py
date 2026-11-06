import hashlib

import httpx
import pytest
import yaml
from fastapi import HTTPException

from jbm_cluster_py.platform.logs import retention


@pytest.mark.asyncio
@pytest.mark.parametrize("reload_succeeds", [True, False])
async def test_save_uses_effective_loki_rules_and_restores_on_failure(tmp_path, monkeypatch, reload_succeeds):
    policy = tmp_path / "overrides.yml"
    original = b"overrides: {}\n"
    policy.write_bytes(original)
    before = {"login": 365, "operation": 365, "access": 365}
    after = {"login": 366, "operation": 367, "access": 368}

    def respond(request):
        if request.url.path == "/config":
            return httpx.Response(200, json={
                "compactor": {"retention_enabled": True},
                "limits_config": {"retention_period": "1y"},
                "runtime_config": {"file": str(policy)},
            })
        assert request.url.path == "/metrics"
        digest = hashlib.sha256(policy.read_bytes() if reload_succeeds else original).hexdigest()
        return httpx.Response(200, text=f'loki_runtime_config_hash{{config="loki",sha256="{digest}"}} 1\n')

    client = httpx.AsyncClient(transport=httpx.MockTransport(respond))
    monkeypatch.setattr(retention.httpx, "AsyncClient", lambda **_: client)

    async def skip_reload_delay(_):
        pass

    monkeypatch.setattr(retention.asyncio, "sleep", skip_reload_delay)
    manager = retention.AuditRetention("http://loki", "platform-security", 365, policy)

    if reload_succeeds:
        result = await manager.save(after, before)
        assert result["days"] == after
        active = yaml.safe_load(policy.read_bytes())["overrides"]["fake"]["retention_stream"]
        assert {rule["selector"]: rule["period"] for rule in active} == {
            retention.SELECTORS[name]: f"{days}d" for name, days in after.items()
        }
    else:
        with pytest.raises(HTTPException, match="尚未确认生效"):
            await manager.save(after, before)
        assert policy.read_bytes() == original
