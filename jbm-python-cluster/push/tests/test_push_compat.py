import asyncio
import base64
import json
import time

from fastapi.testclient import TestClient

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.push.main import create_app
from jbm_cluster_py.platform.push.service import PushService, parse_user_id


def push_config() -> AppConfig:
    return AppConfig(
        {
            "server": {"host": "127.0.0.1", "port": 3313},
            "spring": {
                "application": {"name": "jbm-cluster-platform-push"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
            },
            "integrations": {"telemetry": {"enabled": False}},
        },
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
