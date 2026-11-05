import json
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

import httpx
import pytest
from fastapi import FastAPI, Request

from jbm_cluster_py.common.operation_audit import (
    OperationAuditor,
    OperationAuditMiddleware,
    operation_result,
    safe_values,
)
from jbm_cluster_py.common.security_audit import audit_digest


@pytest.mark.asyncio
async def test_source_network_enforcement_covers_reads_and_ignores_spoofed_forwarding(tmp_path):
    from jbm_cluster_py.common.admin_security import AdminAccessPolicy

    auditor = OperationAuditor(Config(tmp_path))
    auditor.directory.mkdir(parents=True)
    auditor.admin_access = AdminAccessPolicy(
        {
            "mode": "enforce",
            "allowed-networks": ["10.0.0.0/8"],
            "trusted-proxy-networks": ["127.0.0.0/8"],
        }
    )
    app = FastAPI()
    app.state.operation_audit = auditor
    app.add_middleware(OperationAuditMiddleware, auditor=auditor)
    calls = []

    @app.middleware("http")
    async def identity(request, call_next):
        request.state.identity = {
            "userId": 21,
            "userName": "tenant-admin",
            "tenantId": "7",
            "roles": ["tenant_admin"],
        }
        return await call_next(request)

    @app.get("/oauth2/userinfo")
    @app.get("/user")
    async def read():
        calls.append("read")
        return {"success": True}

    @app.post("/user")
    async def write():
        calls.append("write")
        return {"success": True}

    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app, client=("192.0.2.1", 123)), base_url="http://test"
    ) as c:
        assert (await c.get("/user", headers={"X-Forwarded-For": "10.1.1.1"})).status_code == 403
        assert (await c.post("/user", json={})).status_code == 403
        assert (await c.get("/oauth2/userinfo")).status_code == 200
    assert calls == ["read"]
    events = [json.loads(p.read_text(encoding="utf8")) for p in auditor.directory.glob("*.json")]
    assert len(events) == 2 and all(
        row["success"] is False and row["tenantId"] == "7" and row["ip"] == "192.0.2.1"
        for row in events
    )


@pytest.mark.asyncio
@pytest.mark.parametrize("status", [200, 403, 404])
async def test_failed_business_read_is_audited_even_when_http_returns_200(tmp_path, status):
    from starlette.responses import JSONResponse

    auditor = OperationAuditor(Config(tmp_path))
    auditor.directory.mkdir(parents=True)
    app = FastAPI()
    app.add_middleware(OperationAuditMiddleware, auditor=auditor)

    @app.middleware("http")
    async def identity(request, call_next):
        request.state.identity = {"userId": 21, "tenantId": "7"}
        return await call_next(request)

    @app.get("/devices/other")
    async def read():
        return JSONResponse({"success": False, "message": "无权访问"}, status_code=status)

    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://test"
    ) as c:
        response = await c.get("/devices/other")
        assert response.status_code == status
    rows = [json.loads(p.read_text(encoding="utf8")) for p in auditor.directory.glob("*.json")]
    assert (
        len(rows) == 1
        and rows[0]["operationStatus"] == "FAILED"
        and rows[0]["tenantScopeVerified"] is True
    )


def test_batch_acceptance_tracks_every_task_and_group_only_tracks_its_parent():
    outcome, refs, _ = operation_result(
        200,
        json.dumps(
            {
                "success": True,
                "result": [
                    {"taskId": "a", "status": "pending"},
                    {"taskId": "b", "status": "pending"},
                ],
            }
        ).encode(),
    )
    assert outcome == "ACCEPTED" and refs["taskIds"] == ["a", "b"]
    outcome, refs, _ = operation_result(
        200,
        json.dumps(
            {
                "success": True,
                "result": {
                    "requestId": "parent",
                    "status": "accepted",
                    "results": [{"requestId": "child", "status": "queued"}],
                },
            }
        ).encode(),
    )
    assert outcome == "ACCEPTED" and refs["taskIds"] == ["parent"]


def test_json_text_secrets_are_redacted_without_hiding_business_month_keys():
    assert safe_values({"monthKey": "2026-09", "detail": '{"password":"secret-value"}'}) == {
        "monthKey": "2026-09",
        "detail": {"password": "[已脱敏]"},
    }


def test_job_legacy_audit_is_only_deleted_after_verified_archive_receipt(tmp_path):
    from jbm_cluster_py.platform.job.database_backups import DatabaseBackups

    manager = DatabaseBackups(
        str(tmp_path / "job-old"), {"url": "mysql+asyncmy://u:p@localhost/test"}
    )
    try:
        manager.audit("21", "save_policy", "source-1")
        manager.operation_auditor = SimpleNamespace(
            enabled=True,
            service="job",
            _write=Mock(),
            archive_confirmed_sync=Mock(return_value=False),
        )
        manager.flush_legacy_audit()
        with manager.connect() as db:
            assert db.execute("SELECT COUNT(*) FROM audit").fetchone()[0] == 1
        event = manager.operation_auditor._write.call_args.args[0]
        assert (
            event["success"] is None
            and event["phase"] == "LEGACY"
            and event["legacy"]["actor"] == "21"
        )
        manager.operation_auditor.archive_confirmed_sync.return_value = True
        manager.flush_legacy_audit()
        with manager.connect() as db:
            assert db.execute("SELECT COUNT(*) FROM audit").fetchone()[0] == 0
    finally:
        manager.worker_lock.close()


class Config:
    service_name = "jbm-cluster-platform-center"
    kafka = {"enabled": True}

    def __init__(self, path):
        self.path = path

    def get(self, key, default=None):
        return {
            "jbm.security-audit.enabled": True,
            "jbm.security-audit.operation-spool-directory": str(self.path),
        }.get(key, default)


def test_background_publisher_startup_failure_prevents_silent_worker_start(tmp_path):
    auditor = OperationAuditor(Config(tmp_path))
    auditor.prepare = Mock()
    auditor.start = AsyncMock(side_effect=RuntimeError("configuration unavailable"))
    with pytest.raises(RuntimeError, match="worker not started"):
        auditor.start_background()
    assert not auditor.background_thread.is_alive()


@pytest.mark.asyncio
async def test_mutation_has_durable_start_and_actual_result_with_redacted_changes(tmp_path):
    auditor = OperationAuditor(Config(tmp_path))
    auditor.directory.mkdir(parents=True)
    auditor.kafka = SimpleNamespace(publish_json=AsyncMock())
    app = FastAPI()
    app.add_middleware(OperationAuditMiddleware, auditor=auditor)

    @app.middleware("http")
    async def authenticated(request, call_next):
        request.state.identity = {
            "userId": 21,
            "userName": "tenant-admin",
            "tenantId": 7,
            "roles": ["tenant_admin"],
        }
        return await call_next(request)

    @app.put("/user/{user_id}")
    async def update(user_id: int, request: Request):
        body = await request.json()
        # Execution must follow the durable START, not background scheduling.
        assert len(list(auditor.directory.glob("*.json"))) == 1
        return {"success": True, "result": {"id": user_id, "nickName": body["nickName"]}}

    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://test"
    ) as client:
        response = await client.put(
            "/user/22",
            json={
                "nickName": "new",
                "password": "must-not-leak",
                "nested": {"accessToken": "must-not-leak"},
            },
        )
    assert response.status_code == 200 and response.json()["success"] is True
    events = sorted(
        [json.loads(p.read_text(encoding="utf8")) for p in auditor.directory.glob("*.json")],
        key=lambda x: x["phase"],
    )
    assert {e["phase"] for e in events} == {"START", "RESULT"}
    result = next(e for e in events if e["phase"] == "RESULT")
    assert result["operationStatus"] == "SUCCEEDED" and result["success"] is True
    assert result["resourceIds"] == {"user_id": "22"} and result["businessRef"] == {"id": "22"}
    assert all(e["tenantId"] == "7" and e["tenantScopeVerified"] for e in events)
    assert len({e["operationId"] for e in events}) == len({e["requestId"] for e in events}) == 1
    assert "must-not-leak" not in json.dumps(events)
    assert response.headers["x-audit-operation-id"] == result["operationId"]


@pytest.mark.parametrize(
    "status,body,expected",
    [
        (200, {"success": False, "message": "not allowed"}, "FAILED"),
        (403, {}, "FAILED"),
        (200, {"success": True, "result": {"status": "queued", "id": "task-1"}}, "ACCEPTED"),
        (202, {"success": True}, "ACCEPTED"),
        (200, {"success": True, "result": {"status": "failed"}}, "FAILED"),
        (200, {}, "UNKNOWN"),
        (204, {}, "SUCCEEDED"),
    ],
)
def test_outcome_is_business_result_not_http_200(status, body, expected):
    assert operation_result(status, json.dumps(body).encode())[0] == expected


@pytest.mark.asyncio
async def test_persistent_spool_restarts_and_waits_for_log_service_receipt(tmp_path):
    a = OperationAuditor(Config(tmp_path))
    a.directory.mkdir(parents=True)
    event = {"eventId": "a" * 32, "timestamp": 1, "phase": "RESULT", "operationStatus": "FAILED"}
    await a.record(event)
    restarted = OperationAuditor(Config(tmp_path))
    restarted.kafka = SimpleNamespace(publish_json=AsyncMock(side_effect=RuntimeError("offline")))
    restarted.receipts = SimpleNamespace(client=SimpleNamespace(get=AsyncMock(return_value=None)))
    with pytest.raises(RuntimeError):
        await restarted.flush()
    assert len(list(a.directory.glob("*.json"))) == 1
    restarted.kafka.publish_json = AsyncMock()
    await restarted.flush()
    assert list(a.directory.glob("*.json")), "broker ACK alone must not delete the source copy"
    assert restarted.kafka.publish_json.call_args.args[1] == event
    restarted.receipts.client.get.return_value = audit_digest(event)
    await restarted.flush()
    assert not list(a.directory.glob("*.json"))


@pytest.mark.asyncio
async def test_spool_failure_before_execution_denies_mutation():
    app = FastAPI()
    calls = []
    auditor = SimpleNamespace(
        enabled=True,
        service="center",
        proxies=[],
        record=AsyncMock(side_effect=OSError("disk full")),
    )
    app.add_middleware(OperationAuditMiddleware, auditor=auditor)

    @app.middleware("http")
    async def authenticate(request, call_next):
        request.state.identity = {"userId": 1, "userName": "u", "tenantId": 7}
        return await call_next(request)

    @app.post("/user")
    async def mutate():
        calls.append(1)
        return {"success": True}

    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://test"
    ) as c:
        r = await c.post("/user", json={})
    assert r.status_code == 503 and not calls


@pytest.mark.asyncio
async def test_result_transport_failure_does_not_rewrite_executed_success():
    app = FastAPI()
    auditor = SimpleNamespace(
        enabled=True,
        service="center",
        proxies=[],
        pending_results=[],
        record=AsyncMock(side_effect=[None, OSError("disk failed")]),
    )
    app.add_middleware(OperationAuditMiddleware, auditor=auditor)

    @app.middleware("http")
    async def authenticate(request, call_next):
        request.state.identity = {"userId": 1, "tenantId": 7}
        return await call_next(request)

    @app.post("/user")
    async def mutate():
        return {"success": True}

    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app), base_url="http://test"
    ) as c:
        r = await c.post("/user", json={})
    assert r.status_code == 200 and r.json()["success"] is True
    assert (
        len(auditor.pending_results) == 1
        and auditor.pending_results[0]["operationStatus"] == "SUCCEEDED"
    )


def test_configuration_secrets_are_redacted_even_when_named_value():
    assert (
        safe_values({"configKey": "jbm.crypto.encryption-key", "value": "secret-value"})["value"]
        == "[已脱敏]"
    )


def test_late_worker_result_is_idempotent_and_keeps_original_actor(tmp_path):
    auditor = OperationAuditor(Config(tmp_path))
    auditor.directory.mkdir(parents=True)
    context = {
        "eventId": "a" * 32,
        "operationId": "b" * 32,
        "timestamp": 1,
        "source": "operation",
        "category": "operation",
        "phase": "START",
        "userId": 21,
        "account": "tenant-admin",
        "tenantId": "7",
        "tenantScopeVerified": True,
        "requestId": "request-1",
    }
    auditor.complete_sync(context, "FAILED", "task-1", 120000, "任务校验失败")
    auditor.complete_sync(context, "FAILED", "task-1", 120000, "任务校验失败")
    rows = [json.loads(p.read_text(encoding="utf8")) for p in auditor.directory.glob("*.json")]
    assert len(rows) == 1 and rows[0]["phase"] == "RESULT" and rows[0]["success"] is False
    assert (
        rows[0]["userId"] == 21
        and rows[0]["operationId"] == "b" * 32
        and rows[0]["timestamp"] == 120000
    )


def test_job_completion_is_recovered_after_worker_restart(tmp_path):
    from jbm_cluster_py.platform.job.database_backups import DatabaseBackups

    manager = DatabaseBackups(str(tmp_path / "jobs"), {"url": "mysql+asyncmy://u:p@localhost/test"})
    try:
        context = {
            "operationId": "a" * 32,
            "eventId": "b" * 32,
            "timestamp": 1,
            "userId": 21,
            "tenantId": "7",
            "phase": "START",
        }
        task = {
            "id": "task-1",
            "kind": "backup",
            "sourceId": "jbm-default",
            "status": "failed",
            "createdAt": "2026-11-05T01:00:00+00:00",
            "finishedAt": "2026-11-05T01:01:00+00:00",
            "error": "已失败",
            "auditContext": context,
        }
        with manager.connect() as db:
            manager.write_task(db, task)
        manager.operation_auditor = SimpleNamespace(
            enabled=True, complete_sync=Mock(side_effect=OSError("disk full"))
        )
        with pytest.raises(OSError):
            manager.flush_audit_results()
        assert not manager.task("task-1").get("auditFinalRecorded")
        manager.worker_lock.close()
        manager = DatabaseBackups(
            str(tmp_path / "jobs"), {"url": "mysql+asyncmy://u:p@localhost/test"}
        )
        manager.operation_auditor = SimpleNamespace(enabled=True, complete_sync=Mock())
        manager.flush_audit_results()
        manager.flush_audit_results()
        assert manager.task("task-1")["auditFinalRecorded"] is True
        manager.operation_auditor.complete_sync.assert_called_once()
    finally:
        manager.worker_lock.close()
