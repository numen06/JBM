import json

import httpx
import pytest
from fastapi.testclient import TestClient

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.push import ntfy
from jbm_cluster_py.platform.push.main import create_app
from jbm_cluster_py.platform.push.service import PushService


def config(token="tk_test_secret"):
    return {"serverUrl": "https://notify.example.com", "topicPrefix": "jbm", "token": token, "priority": 3}


def transport(monkeypatch, handler):
    original = httpx.AsyncClient
    monkeypatch.setattr(ntfy.httpx, "AsyncClient", lambda **kwargs: original(transport=httpx.MockTransport(handler), **kwargs))


async def test_publish_routes_masked_config_edit_and_persisted_delivery(monkeypatch):
    sent = []

    def handler(request):
        sent.append(request)
        return httpx.Response(200, json={"id": "ntfy-message-1", "event": "message"})

    transport(monkeypatch, handler)
    service = PushService()
    saved = await service.save_push_config({"type": 8, "enable": True, "releaseContent": json.dumps(config())})
    assert "tk_test_secret" not in json.dumps(saved)
    edited = json.loads(saved["releaseContent"])
    edited["priority"] = 4
    await service.save_push_config({**saved, "releaseContent": json.dumps(edited)})
    result = await service.publish_message({"pushWay": "ntfy", "ntfyConfigId": saved["id"], "title": "中文标题", "content": "路灯告警", "syncDelivery": True}, 1)
    assert result["deliveryStatus"] == "sent"
    assert str(sent[0].url) == "https://notify.example.com"
    assert sent[0].headers["authorization"] == "Bearer tk_test_secret"
    assert json.loads(sent[0].content) == {"topic": "jbm-user-1", "title": "中文标题", "message": "路灯告警", "priority": 4}
    assert service.messages[0]["pushWay"] == "ntfy"
    assert service.messages[0]["extend"]["messageId"] == "ntfy-message-1"
    assert "tk_test_secret" not in json.dumps(await service.list_push_configs({}))
    assert "tk_test_secret" not in json.dumps(await service.list_push_config_rows({}))
    assert "tk_test_secret" not in json.dumps(service.messages)


@pytest.mark.parametrize("failure", [403, "timeout", "invalid_reply"])
async def test_remote_failures_are_not_reported_as_sent_or_leaked(monkeypatch, failure):
    def handler(request):
        if failure == "timeout":
            raise httpx.ReadTimeout("tk_test_secret", request=request)
        if failure == "invalid_reply":
            return httpx.Response(200, json={"error": "tk_test_secret"})
        return httpx.Response(failure, text="tk_test_secret")

    transport(monkeypatch, handler)
    service = PushService()
    await service.save_push_config({"type": 8, "enable": True, "releaseContent": json.dumps(config())})
    result = await service.publish_message({"pushWay": "ntfy", "content": "test", "syncDelivery": True}, 1)
    assert result["deliveryStatus"] == "failed"
    assert service.messages[0]["pushStatus"] == "fail"
    assert "tk_test_secret" not in json.dumps([result, service.messages])


async def test_disabled_missing_and_selected_configs(monkeypatch):
    def no_http(request):
        pytest.fail("disabled/missing channel must not send")

    transport(monkeypatch, no_http)
    service = PushService()
    disabled = await service.save_push_config({"type": 8, "enable": False, "releaseContent": json.dumps(config())})
    await service.save_push_config({"type": 8, "enable": True, "releaseContent": json.dumps(config())})
    for config_id in (disabled["id"], 999):
        result = await service.publish_message({"pushWay": "ntfy", "ntfyConfigId": config_id, "content": "test", "syncDelivery": True}, 1)
        assert result["deliveryStatus"] == "failed"


@pytest.mark.parametrize("change", [{"serverUrl": "file:///etc/passwd"}, {"serverUrl": "https://user:password@host"}, {"topicPrefix": "bad/topic"}, {"priority": 6}, {"token": "secret\nheader"}])
def test_invalid_configuration_rejected(change):
    with pytest.raises(ValueError):
        ntfy.validate_config({**config(), **change})


async def test_standard_recipients_queue_delivery_and_broadcast(monkeypatch):
    sent = []

    def handler(request):
        sent.append(json.loads(request.content))
        return httpx.Response(200, json={"id": str(len(sent)), "event": "message"})

    transport(monkeypatch, handler)
    service = PushService()
    await service.save_push_config({"type": 8, "enable": True, "releaseContent": json.dumps(config())})
    await service.publish_message({"pushWay": "ntfy", "recUserIds": [101, 202, 101], "content": "personal", "syncDelivery": True}, 999)
    # Direct queue events must also fan out their standard recipient list.
    await service.handle_push_event({"pushWay": "ntfy", "recUserIds": [303, 404], "content": "queued"})
    await service.publish_message({"pushWay": "ntfy", "recUserId": 0, "content": "broadcast", "syncDelivery": True}, 999)
    assert [r["topic"] for r in sent] == ["jbm-user-101", "jbm-user-202", "jbm-user-303", "jbm-user-404", "jbm-broadcast"]
    assert [r["recUserId"] for r in service.messages] == [0, 404, 303, 202, 101]
    failed = await service.publish_message({"pushWay": "ntfy", "recUserId": 101, "topic": "jbm-user-202", "content": "override", "syncDelivery": True}, 999)
    assert failed["deliveryStatus"] == "failed"
    assert len(sent) == 5
    invalid = await service.handle_push_event({"pushWay": "ntfy", "recUserIds": ["invalid"], "content": "invalid recipient"})
    assert invalid["deliveryStatus"] == "failed"
    assert len(sent) == 5


async def test_mixed_recipient_failure_is_not_reported_as_all_sent(monkeypatch):
    def handler(request):
        topic = json.loads(request.content)["topic"]
        return httpx.Response(403) if topic == "jbm-user-202" else httpx.Response(200, json={"id": "ok", "event": "message"})
    transport(monkeypatch, handler)
    service = PushService()
    await service.save_push_config({"type": 8, "enable": True, "releaseContent": json.dumps(config())})
    result = await service.publish_message({"pushWay": "ntfy", "recUserIds": [101, 202], "content": "mixed", "syncDelivery": True}, 999)
    assert result["deliveryStatus"] == "failed"
    assert [r["deliveryStatus"] for r in result["deliveries"]] == ["sent", "failed"]


def test_http_config_and_send_survive_application_restart(tmp_path, monkeypatch):
    transport(monkeypatch, lambda request: httpx.Response(200, json={"id": "saved-message", "event": "message"}))
    app_config = AppConfig({"spring": {"application": {"name": "push"}, "datasource": {"url": "sqlite+aiosqlite:///" + str(tmp_path / "push.db")}}, "jbm": {"push": {"security": {"enabled": False, "dev-user-id": 1}}}}, profile="test", config_dir=None, app="push")
    with TestClient(create_app(app_config)) as client:
        r = client.post("/pushConfigInfo/save", json={"type": 8, "enable": True, "releaseContent": json.dumps(config())})
        assert r.status_code == 200
        assert "tk_test_secret" not in r.text
        assert client.post("/pushConfigInfo/save", json={"type": 8, "releaseContent": "invalid"}).status_code == 400
    with TestClient(create_app(app_config)) as client:
        response = client.post("/notification/send/ntfy", json={"title": "test", "content": "hello", "syncDelivery": True})
        assert response.json()["result"]["deliveryStatus"] == "sent"
        records = client.post("/pushMessage/pageList", json={"pushWay": "ntfy"}).json()["result"]["contents"]
        assert records[0]["extend"]["messageId"] == "saved-message"
