import json
from unittest.mock import AsyncMock

import httpx
import pytest

from jbm_cluster_py.platform.logs.loki import LokiSink
from jbm_cluster_py.platform.logs.security_audit import SecurityAuditStore, audit_query_scope


@pytest.mark.asyncio
async def test_grouped_query_verifies_all_facts_before_grouping_filtering_and_pagination(tmp_path):
    sink = LokiSink({"enabled": True, "url": "http://test/loki/api/v1/push", "username": "test", "password": "test"})
    store = SecurityAuditStore({"integrity-key": "x" * 32, "backup-directory": str(tmp_path)}, loki=sink)
    events = []
    for number, (op, phase, stamp, status) in enumerate([
        ("a", "START", 100, "STARTED"), ("a", "RESULT", 100, "SUCCEEDED"),
        ("b", "START", 200, "STARTED"), ("b", "RESULT", 201, "FAILED"),
        ("c", "START", 300, "STARTED"),
    ]):
        event = {"eventId": f"{number:032x}", "timestamp": stamp, "operationId": op,
                 "phase": phase, "operationStatus": status, "tenantId": "7",
                 "userId": "21", "tenantScopeVerified": True, "category": "operation",
                 "success": True if status == "SUCCEEDED" else False if status == "FAILED" else None}
        if phase == "START":
            event["submittedChanges"] = {"name": op}
        events.append(event)
    def rows():
        return [[str(e["timestamp"] * 1000000), json.dumps({**e, "integritySignature": store.envelope(e)[1]})] for e in reversed(events)]
    store._range = AsyncMock(side_effect=lambda *args: rows())
    query = audit_query_scope({"category": "operation", "groupOperations": True, "size": 1}, "7")
    result = await store.query(query)
    assert result["total"] == 3 and result["records"][0]["operationId"] == "c"
    assert 'tenantId = "7"' in store._range.call_args.args[0]
    assert 'tenantScopeVerified = "true"' in store._range.call_args.args[0]
    assert store._range.call_args.args[3] == 4901
    result = await store.query({**query, "page": 2})
    assert result["records"][0]["operationId"] == "b"
    result = await store.query({**query, "success": True})
    row = result["records"][0]
    assert result["total"] == 1 and row["phase"] == "RESULT"
    assert row["operationSummary"]["submittedChanges"] == {"name": "a"}
    assert "submittedChanges" not in row  # Representative signed fact is unchanged.
    assert "success =" not in store._range.call_args.args[0]
    original_rows = rows()
    changed = json.loads(original_rows[-1][1])
    changed["account"] = "forged"
    original_rows[-1][1] = json.dumps(changed)
    store._range = AsyncMock(return_value=original_rows)
    with pytest.raises(ValueError, match="完整性"):
        await store.query(query)
    store._range = AsyncMock(return_value=[["1", "{}"]] * 4901)
    with pytest.raises(ValueError, match="缩小"):
        await store.query(query)
    await sink.stop()


@pytest.mark.asyncio
async def test_partial_async_results_never_appear_as_a_success(tmp_path):
    sink = LokiSink({"enabled": True, "url": "http://test/loki/api/v1/push", "username": "test", "password": "test"})
    store = SecurityAuditStore({"integrity-key": "x" * 32, "backup-directory": str(tmp_path)}, loki=sink)
    base = {"tenantId": "7", "userId": "21", "operationId": "a", "category": "operation"}
    events = [
        {**base, "eventId": "a"*32, "timestamp": 100, "phase": "ACCEPTED", "businessRef": {"taskIds": ["one", "two"]}},
        {**base, "eventId": "b"*32, "timestamp": 200, "phase": "RESULT", "businessRef": {"taskId": "one"}, "success": True, "operationStatus": "SUCCEEDED"},
    ]
    store._range = AsyncMock(side_effect=lambda *args: [
        [str(e["timestamp"] * 1000000), json.dumps({**e, "integritySignature": store.envelope(e)[1]})] for e in events])
    query = {"category": "operation", "groupOperations": True}
    result = await store.query(query)
    assert result["records"][0]["operationSummary"]["status"] == "ACCEPTED"
    assert (await store.query({**query, "success": True}))["total"] == 0
    events.append({**base, "eventId": "c"*32, "timestamp": 300, "phase": "RESULT", "businessRef": {"taskId": "two"}, "success": False, "operationStatus": "FAILED"})
    assert (await store.query({**query, "success": False}))["total"] == 1
    await sink.stop()
