import asyncio
import base64
import json
import smtplib
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.push.config_repository import PushConfigRepository
from jbm_cluster_py.platform.push.main import _ensure_system_channel_configs, create_app
from jbm_cluster_py.platform.push.push_message_repository import PushMessageRepository
from jbm_cluster_py.platform.push.service import PushService, parse_user_id


def push_config(database_url: str | None = None) -> AppConfig:
    data = {
        "server": {"host": "127.0.0.1", "port": 3313},
        "spring": {
            "application": {"name": "jbm-cluster-platform-push"},
            "cloud": {"nacos": {"discovery": {"enabled": False}}},
        },
        "integrations": {"telemetry": {"enabled": False}},
    }
    if database_url:
        data["spring"]["datasource"] = {"url": database_url}
    return AppConfig(
        data,
        profile="test",
        config_dir=None,
        app="push",
    )


def test_push_message_test_send_page_read_and_ack() -> None:
    with TestClient(create_app(push_config())) as client:
        sent = client.post(
            "/pushTest/send",
            json={"recUserIds": [1], "title": "hello", "content": "world", "showInMessageCenter": True},
        )
        assert sent.status_code == 200
        task = sent.json()["result"]
        assert task["sentCount"] == 1

        page = client.post("/pushMessage/findCurrMessagePage", json={"pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert rows[0]["title"] == "hello"
        assert rows[0]["readFlag"] is False

        count = client.post("/pushMessage/unreadCount", json={})
        assert count.json()["result"] == 1

        msg_id = rows[0]["msgId"]
        read = client.post("/pushMessage/read", json={"ids": [msg_id]})
        assert read.json()["success"] is True

        ack = client.post("/pushTest/ack", json={"testRunId": task["taskId"], "msgId": msg_id, "latencyMs": 12})
        assert ack.json()["result"]["ackCount"] == 1
        duplicate_ack = client.post("/pushTest/ack", json={"testRunId": task["taskId"], "msgId": msg_id, "latencyMs": 18})
        assert duplicate_ack.json()["result"]["ackCount"] == 1


def test_push_config_crud_paths() -> None:
    with TestClient(create_app(push_config())) as client:
        saved = client.post("/pushConfigInfo/save", json={"enable": True, "type": 1, "releaseContent": "demo"})
        config_id = saved.json()["result"]["id"]
        page = client.post("/pushConfigInfo/pageList", json={"pageForm": {"currPage": 1, "pageSize": 10}})
        assert page.json()["result"]["total"] == 1
        deleted = client.post("/pushConfigInfo/deleteByIds", json={"ids": [config_id]})
        assert deleted.json()["result"] is True

        email = client.post("/emailPushConfig/save", json={"entity": {"host": "smtp.local", "username": "demo"}})
        assert email.json()["result"]["host"] == "smtp.local"


def test_external_notification_records_failed_status_without_channel_config() -> None:
    with TestClient(create_app(push_config())) as client:
        sent = client.post(
            "/notification/send/email",
            json={"recUserId": 1, "title": "mail", "content": "body"},
        )
        assert sent.json()["success"] is True

        page = client.post("/pushMessage/pageList", json={"pushWay": "email", "pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert rows[0]["title"] == "mail"
        assert rows[0]["pushStatus"] == "fail"
        assert rows[0]["extend"]["deliveryStatus"] == "failed"


def test_email_notification_uses_smtp_channel_config(monkeypatch: pytest.MonkeyPatch) -> None:
    sent_messages = []
    logins = []

    class FakeSMTP:
        def __init__(self, host: str, port: int, timeout: int) -> None:
            self.host = host
            self.port = port
            self.timeout = timeout

        def __enter__(self) -> "FakeSMTP":
            return self

        def __exit__(self, exc_type, exc, tb) -> None:
            return None

        def login(self, username: str, password: str) -> None:
            logins.append((username, password))

        def send_message(self, message) -> None:
            sent_messages.append(message)

    monkeypatch.setattr(smtplib, "SMTP_SSL", FakeSMTP)
    config = AppConfig(
        {
            "server": {"host": "127.0.0.1", "port": 3313},
            "spring": {
                "application": {"name": "jbm-cluster-platform-push"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
                "mail": {
                    "host": "smtp.example.com",
                    "username": "sender@example.com",
                    "password": "mail-pass",
                    "port": 465,
                },
            },
            "integrations": {"telemetry": {"enabled": False}},
        },
        profile="test",
        config_dir=None,
        app="push",
    )

    with TestClient(create_app(config)) as client:
        sent = client.post(
            "/notification/send/email",
            json={
                "recUserId": 1,
                "receiver": "receiver@example.com",
                "title": "mail",
                "content": "body",
            },
        )
        assert sent.json()["success"] is True

        page = client.post("/pushMessage/pageList", json={"pushWay": "email", "pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]

    assert logins == [("sender@example.com", "mail-pass")]
    assert len(sent_messages) == 1
    assert sent_messages[0]["To"] == "receiver@example.com"
    assert sent_messages[0]["Subject"] == "mail"
    assert rows[0]["pushStatus"] == "issued"
    assert rows[0]["extend"]["deliveryStatus"] == "sent"
    assert rows[0]["extend"]["host"] == "smtp.example.com"


def test_sms_notification_uses_jaja7_dry_run_channel() -> None:
    service = PushService(push_config={"jaja7-dry-run": True})

    asyncio.run(
        service.handle_push_event(
            {
                "pushWay": "sms",
                "recUserId": 1,
                "title": "sms",
                "content": "验证码",
                "phoneNumber": "13585658904",
                "templateCode": "SMS_236340338",
                "params": {"code": "123456"},
                "signName": "甲佳智能",
            }
        )
    )

    assert service.messages[0]["pushStatus"] == "issued"
    assert service.messages[0]["extend"]["deliveryStatus"] == "dry-run"
    assert service.messages[0]["extend"]["phoneNumber"] == "135****8904"
    assert "123456" not in json.dumps(service.messages[0], ensure_ascii=False)


def test_sms_sync_delivery_bypasses_rabbitmq_queue() -> None:
    class FakeRabbitMQ:
        enabled = True

        def __init__(self) -> None:
            self.published = []

        async def publish_json(self, queue: str, payload: dict) -> None:
            self.published.append((queue, payload))

    rabbitmq = FakeRabbitMQ()
    service = PushService(rabbitmq=rabbitmq, push_config={"jaja7-dry-run": True})

    queued = asyncio.run(
        service.publish_message(
            {
                "pushWay": "sms",
                "recUserId": 1,
                "title": "sms",
                "content": "验证码",
                "phoneNumber": "13585658904",
                "templateCode": "SMS_236340338",
                "params": {"code": "123456"},
                "signName": "甲佳智能",
            },
            0,
        )
    )
    assert queued["deliveryStatus"] == "queued"
    assert len(rabbitmq.published) == 1

    synced = asyncio.run(
        service.publish_message(
            {
                "pushWay": "sms",
                "recUserId": 1,
                "title": "sms",
                "content": "验证码",
                "phoneNumber": "13585658904",
                "templateCode": "SMS_236340338",
                "params": {"code": "654321"},
                "signName": "甲佳智能",
                "syncDelivery": True,
            },
            0,
        )
    )
    assert synced["deliveryStatus"] == "dry-run"
    assert len(rabbitmq.published) == 1
    assert service.messages[-1]["extend"]["deliveryStatus"] == "dry-run"


def test_pin_send_calls_sms_provider_when_dry_run_disabled(monkeypatch: pytest.MonkeyPatch) -> None:
    sent_payloads = []

    async def fake_send_aliyun_sms(self, payload, config):  # type: ignore[no-untyped-def]
        sent_payloads.append((dict(payload), dict(config)))
        return {"Code": "OK", "RequestId": "req-pin"}

    monkeypatch.setattr(PushService, "_send_aliyun_sms", fake_send_aliyun_sms)
    config = AppConfig(
        {
            "server": {"host": "127.0.0.1", "port": 3313},
            "spring": {
                "application": {"name": "jbm-cluster-platform-push"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
            },
            "aliyun": {
                "sms": {
                    "accessKeyId": "ak",
                    "accessKeySecret": "secret",
                    "signName": "甲佳智能",
                }
            },
            "jbm": {"push": {"jaja7-dry-run": False}},
            "integrations": {"telemetry": {"enabled": False}},
        },
        profile="test",
        config_dir=None,
        app="push",
    )

    with TestClient(create_app(config)) as client:
        response = client.post(
            "/pin/send",
            params={
                "phoneNumber": "13585658904",
                "code": "123456",
                "templateCode": "SMS_236340338",
                "signName": "甲佳智能",
            },
        )

    assert response.json()["success"] is True
    assert response.json()["message"] == "短信验证码发送成功"
    assert response.json()["result"]["Code"] == "OK"
    assert response.json()["result"]["pin"] == "123456"
    assert sent_payloads[0][0]["phoneNumber"] == "13585658904"
    assert sent_payloads[0][0]["templateCode"] == "SMS_236340338"
    assert sent_payloads[0][0]["params"]["code"] == "123456"


@pytest.mark.asyncio
async def test_nacos_style_system_channel_config_is_seeded_to_database(tmp_path: Path) -> None:
    repo = PushConfigRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'push-configs.db'}"})
    await repo.start()
    app_config = AppConfig(
        {
            "aliyun": {
                "sms": {
                    "accessKeyId": "ak",
                    "accessKeySecret": "secret",
                    "signName": "甲佳智能",
                }
            },
            "spring": {
                "mail": {"host": "smtp.126.com", "username": "numen_smtp@126.com", "password": "mail-pass", "port": 465},
                "mqtt": {"url": "tcp://mqtt:1883", "username": "mqttId", "password": "mqtt-pass"},
            },
            "jbm": {"push": {"jaja7-dry-run": True}},
        },
        profile="test",
        config_dir=None,
        app="push",
    )

    try:
        await _ensure_system_channel_configs(repo, app_config)
        push_rows = await repo.list_push_configs({})
        email_rows = await repo.list_email_configs({})
    finally:
        await repo.stop()

    by_type = {row["type"]: row for row in push_rows}
    assert {2, 3, 6}.issubset(by_type)
    sms_content = json.loads(by_type[3]["releaseContent"])
    assert sms_content["accessKeyId"] == "ak"
    assert sms_content["jaja7-dry-run"] is True
    assert email_rows[0]["host"] == "smtp.126.com"


def test_current_user_id_is_parsed_from_satoken_login_id() -> None:
    payload = {"loginId": "normal:1000:2057849052900044802"}
    encoded = base64.urlsafe_b64encode(json.dumps(payload).encode("utf-8")).decode("utf-8").rstrip("=")
    assert parse_user_id("Bearer header.%s.signature" % encoded) == 2057849052900044802


def test_self_send_is_visible_to_current_user_from_login_id() -> None:
    payload = {"loginId": "normal:1000:2057849052900044802"}
    encoded = base64.urlsafe_b64encode(json.dumps(payload).encode("utf-8")).decode("utf-8").rstrip("=")
    headers = {"Authorization": "Bearer header.%s.signature" % encoded}
    with TestClient(create_app(push_config())) as client:
        sent = client.post(
            "/pushTest/send",
            headers=headers,
            json={
                "recUserIds": ["2057849052900044802"],
                "title": "self message",
                "content": "visible",
                "showInMessageCenter": True,
            },
        )
        assert sent.json()["result"]["sentCount"] == 1

        page = client.post(
            "/pushMessage/findCurrMessagePage",
            headers=headers,
            json={"pageForm": {"currPage": 1, "pageSize": 10}},
        )
        rows = page.json()["result"]["contents"]
        assert rows[0]["title"] == "self message"


def test_single_rec_user_id_and_extend_visibility_are_supported() -> None:
    with TestClient(create_app(push_config())) as client:
        sent = client.post(
            "/pushTest/send",
            json={
                "recUserId": "7",
                "title": "single receiver",
                "content": "visible",
                "extend": {"showInMessageCenter": "true"},
            },
        )
        assert sent.json()["result"]["sentCount"] == 1

        page = client.post("/pushMessage/pageList", json={"recUserId": 7, "pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert rows[0]["recUserId"] == 7


def test_perf_task_runs_in_background_with_progress() -> None:
    with TestClient(create_app(push_config())) as client:
        started = client.post(
            "/pushTest/perf",
            json={
                "recUserIds": [1],
                "title": "perf",
                "messageCount": 4,
                "batchSize": 1,
                "intervalMillis": 50,
                "showInMessageCenter": True,
            },
        )
        task = started.json()["result"]
        assert task["status"] == "RUNNING"
        task_id = task["taskId"]

        time.sleep(0.06)
        running = client.get(f"/pushTest/perf/{task_id}").json()["result"]
        assert running["status"] == "RUNNING"
        assert 0 < running["sentCount"] < 4

        time.sleep(0.25)
        finished = client.get(f"/pushTest/perf/{task_id}").json()["result"]
        assert finished["status"] == "FINISHED"
        assert finished["sentCount"] == 4

        page = client.post("/pushMessage/findCurrMessagePage", json={"pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert len([row for row in rows if row["title"].startswith("perf")]) == 4


def test_websocket_accepts_browser_stomp_frames_and_heartbeats() -> None:
    with TestClient(create_app(push_config())) as client:
        with client.websocket_connect("/ws", subprotocols=["v12.stomp", "v11.stomp", "v10.stomp"]) as websocket:
            assert websocket.accepted_subprotocol == "v12.stomp"
            websocket.send_text("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\x00")
            connected = websocket.receive_text()
            assert connected.startswith("CONNECTED")
            assert "heart-beat:0,0" in connected

            websocket.send_text("\n")
            websocket.send_text("SUBSCRIBE\nid:sub-0\ndestination:/user/queue/messages\n\n\x00")

            sent = client.post(
                "/pushTest/send",
                json={"recUserIds": [1], "title": "ws", "content": "browser", "showInMessageCenter": False},
            )
            assert sent.json()["result"]["sentCount"] == 1

            message = websocket.receive_text()
            assert message.startswith("MESSAGE")
            assert "subscription:sub-0" in message
            assert '"title":"ws"' in message


def test_broadcast_only_targets_matching_subscriber_user() -> None:
    class DummyWebSocket:
        def __init__(self) -> None:
            self.frames: list[str] = []

        async def send_text(self, frame: str) -> None:
            self.frames.append(frame)

    service = PushService()
    user_one = DummyWebSocket()
    user_two = DummyWebSocket()
    service.add_subscriber(user_one, "sub-1", 1)
    service.add_subscriber(user_two, "sub-2", 2)

    asyncio.run(service.broadcast({"recUserId": 1, "title": "targeted"}))

    assert len(user_one.frames) == 1
    assert "subscription:sub-1" in user_one.frames[0]
    assert user_two.frames == []


@pytest.mark.asyncio
async def test_message_persistence_survives_service_restart(tmp_path: Path) -> None:
    db_path = tmp_path / "push-messages.db"
    database_url = f"sqlite+aiosqlite:///{db_path}"
    config = push_config(database_url)

    with TestClient(create_app(config)) as client:
        sent = client.post(
            "/pushTest/send",
            json={"recUserIds": [42], "title": "persisted", "content": "after restart", "showInMessageCenter": True},
        )
        assert sent.status_code == 200
        assert sent.json()["result"]["sentCount"] == 1

        page = client.post("/pushMessage/pageList", json={"recUserId": 42, "pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert rows[0]["title"] == "persisted"
        assert rows[0]["pushStatus"] == "issued"

    restarted = create_app(config)
    with TestClient(restarted) as client:
        page = client.post("/pushMessage/pageList", json={"recUserId": 42, "pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert len(rows) == 1
        assert rows[0]["title"] == "persisted"

        msg_id = rows[0]["msgId"]
        read = client.post("/pushMessage/read", json={"ids": [msg_id]})
        assert read.json()["success"] is True
        unread = client.post("/pushMessage/unreadCount", json={})
        assert unread.json()["result"] == 0


@pytest.mark.asyncio
async def test_push_message_repository_filters_source_type(tmp_path: Path) -> None:
    repo = PushMessageRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'filters.db'}"})
    await repo.start()
    await repo.save_message(
        {
            "msgId": "sys-1",
            "recUserId": 1,
            "sysMsg": True,
            "title": "system",
            "content": "sys",
            "pushWay": "internal",
            "pushStatus": "issued",
            "readFlag": False,
        }
    )
    await repo.save_message(
        {
            "msgId": "user-1",
            "recUserId": 1,
            "sendUserId": 9,
            "sysMsg": False,
            "title": "user",
            "content": "usr",
            "pushWay": "internal",
            "pushStatus": "issued",
            "readFlag": False,
        }
    )
    system_page = await repo.page_messages({"sourceType": "system", "pageForm": {"currPage": 1, "pageSize": 10}})
    assert system_page["total"] == 1
    assert system_page["contents"][0]["msgId"] == "sys-1"
    await repo.stop()
