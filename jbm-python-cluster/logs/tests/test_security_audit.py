import json
from unittest.mock import AsyncMock
import httpx

import pytest
from sqlalchemy import text

from jbm_cluster_py.common.security_audit import AuditOutbox, audit_event
from jbm_cluster_py.platform.logs.security_audit import SecurityAuditStore
from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.security_audit import audit_reader_scope, audit_query_scope
from fastapi import HTTPException
from starlette.datastructures import Headers
from jbm_cluster_py.common.security_audit import login_succeeded


def test_mysql_bit_and_tinyint_login_result_have_the_same_meaning():
    for value in (b"\x01", memoryview(b"\x01"), 1, "1", True):
        assert login_succeeded(value) is True
    for value in (b"\x00", 0, "0", False):
        assert login_succeeded(value) is False
    assert login_succeeded(None) is None
    assert login_succeeded("unknown") is None


def test_tenant_admin_scope_is_server_bound_and_global_admin_is_unrestricted():
    tenant = {"userId": 2100, "tenantId": 1, "roles": ["tenant_admin"]}
    assert audit_reader_scope(tenant, Headers()) == "1"
    assert audit_query_scope({"tenantScopeVerified": False}, "1") == {"tenantId": "1", "tenantScopeVerified": True}
    assert audit_query_scope({"tenantId": 1}, "1")["tenantScopeVerified"] is True
    for name in ("tenantId", "X-Tenant-Id", "X-Resource-Tenant-Id"):
        with pytest.raises(HTTPException):
            audit_reader_scope(tenant, Headers({name: "2"}))
    for body in ({"tenantId": "2"}, {"tenantId": ["1", "2"]}):
        with pytest.raises(HTTPException):
            audit_query_scope(body, "1")
    for actor in ({"roles": ["tenant_admin"]}, {"tenantId": 1, "roles": ["business_user"]}, {"roles": ["security_auditor"]}):
        with pytest.raises(HTTPException):
            audit_reader_scope(actor, Headers())
    assert audit_reader_scope({"admin": True, "tenantId": 1}, Headers()) is None
    assert audit_reader_scope({"roles": ["security_auditor"], "permissions": ["ACTION_security.audit.read"]}, Headers()) is None


@pytest.mark.asyncio
async def test_loki_query_requires_verified_tenant_scope_even_for_root_owned_records(tmp_path):
    sink = loki_sink()
    store = SecurityAuditStore({"integrity-key": "x" * 32, "backup-directory": str(tmp_path)}, loki=sink)
    store._range = AsyncMock(return_value=[])
    async def count(request):
        return httpx.Response(200, json={"status": "success", "data": {"result": []}})
    await sink.client.aclose()
    sink.client = httpx.AsyncClient(transport=httpx.MockTransport(count))
    await store.query(audit_query_scope({}, "1"))
    selector = store._range.call_args.args[0]
    assert '| tenantId = "1"' in selector
    assert '| tenantScopeVerified = "true"' in selector
    await store.query(audit_query_scope({}, None))
    assert "tenantScopeVerified" not in store._range.call_args.args[0]
    await sink.stop()


def loki_sink():
    sink = LokiSink({"enabled": True, "url": "http://test/loki/api/v1/push", "username": "test", "password": "test"})
    async def handler(request):
        return httpx.Response(204)
    sink.client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    return sink


@pytest.mark.asyncio
async def test_outbox_survives_delivery_failure_and_only_archive_ack_cleans_it(tmp_path):
    kafka = type("Kafka", (), {"enabled": True, "publish_json": AsyncMock(side_effect=ConnectionError("offline"))})()
    db = {"url": "sqlite+aiosqlite:///" + (tmp_path / "queue.db").as_posix()}
    outbox = AuditOutbox({"enabled": True}, kafka, db)
    await outbox.start()
    await outbox.stop()
    event = audit_event("auth", userId="1", operation="LOGIN", password="must-not-store")
    assert "password" not in event
    await outbox.record(event)
    with pytest.raises(ConnectionError):
        await outbox.flush()
    kafka.publish_json.side_effect = None
    await outbox.flush()
    async with outbox.engine.connect() as conn:
        assert (await conn.execute(text("SELECT COUNT(*) FROM security_audit_outbox"))).scalar() == 1
    sink = loki_sink()
    store = SecurityAuditStore({"enabled": True, "integrity-key": "x" * 32, "backup-directory": str(tmp_path / "backup")}, loki=sink, outbox_engine=outbox.engine)
    saved = []
    async def find(_event):
        if not saved:
            saved.append({**event, "integritySignature": store.envelope(event)[1]})
            return []
        return saved
    store._find = find
    await store.handle(event)
    await store.handle(event)  # Kafka redelivery is idempotent, backup is not overwritten.
    async with outbox.engine.connect() as conn:
        assert (await conn.execute(text("SELECT COUNT(*) FROM security_audit_outbox"))).scalar() == 0
    assert len(list((tmp_path / "backup").rglob("*.json"))) == 1
    await store.stop()
    await sink.stop()
    await outbox.engine.dispose()


@pytest.mark.asyncio
async def test_tampering_and_archive_failure_do_not_erase_outbox(tmp_path):
    config = {"enabled": True, "integrity-key": "x" * 32, "backup-directory": str(tmp_path)}
    sink = loki_sink()
    store = SecurityAuditStore(config, loki=sink)
    event = audit_event("gateway", userId="1", tenantId="2", operation="PUT", target="/user/3/roles", success=False)
    store._find = AsyncMock(return_value=[])
    # An acknowledged insert without matching readback must fail.
    with pytest.raises(ValueError, match="回读"):
        await store.handle(event)
    changed = {**event, "success": True}
    with pytest.raises(ValueError, match="冲突"):
        await store.handle(changed)
    payload, signature = store.envelope(event)
    with pytest.raises(ValueError, match="完整性"):
        store.verify(payload + " ", signature)
    rotated = SecurityAuditStore({**config, "integrity-key-id": "v2", "integrity-key": "y" * 32, "verification-keys": {"v1": "x" * 32}}, loki=sink)
    rotated.verify(payload, signature)
    await store.stop()
    await rotated.stop()
    await sink.stop()


def test_no_short_retention_or_default_integrity_secret():
    with pytest.raises(ValueError):
        SecurityAuditStore({"keep-days": 30}, loki=loki_sink())


@pytest.mark.asyncio
async def test_bulk_backup_verifies_every_fact_and_preserves_it_on_redelivery(tmp_path):
    sink = loki_sink()
    store = SecurityAuditStore({"enabled": True, "integrity-key": "x" * 32, "backup-directory": str(tmp_path)}, loki=sink)
    saved, pushes = [], []
    async def transport(request):
        body = json.loads(request.content)
        pushes.append(body)
        assert len(list(tmp_path.rglob('*.json'))) == 100
        for stream in body['streams']:
            saved.extend(stream['values'])
        return httpx.Response(204)
    await sink.client.aclose()
    sink.client = httpx.AsyncClient(transport=httpx.MockTransport(transport))
    store._range = AsyncMock(side_effect=lambda *args: list(saved))
    events = [audit_event('legacy-login', userId=i, operation='PASSWORD', success=True) for i in range(100)]
    await store.handle_many(events)
    await store.handle_many(events)
    assert len(pushes) == 1 and len(saved) == 100
    for event in events:
        envelope = json.loads(store.backup_path(event).read_text())
        store.verify(envelope['payload'], envelope['signature'])
    with pytest.raises(ValueError, match='冲突'):
        await store.handle_many([{**events[0], 'success': False}])
    assert len(pushes) == 1
    await sink.stop()
