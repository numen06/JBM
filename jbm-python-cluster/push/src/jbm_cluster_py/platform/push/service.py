from __future__ import annotations

import base64
import asyncio
import json
import time
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, Iterable, List, Optional

from jbm_cluster_py.common.result import page_result


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def current_millis() -> int:
    return int(time.time() * 1000)


def parse_user_id(authorization: Optional[str]) -> int:
    if not authorization:
        return 1
    token = authorization.replace("Bearer ", "", 1).strip()
    parts = token.split(".")
    if len(parts) < 2:
        return 1
    payload = parts[1] + "=" * (-len(parts[1]) % 4)
    try:
        data = json.loads(base64.urlsafe_b64decode(payload.encode("utf-8")).decode("utf-8"))
    except Exception:
        return 1
    login_id = str(data.get("loginId") or "")
    if ":" in login_id:
        try:
            return int(login_id.rsplit(":", 1)[-1])
        except (TypeError, ValueError):
            pass
    for key in ("userId", "user_id", "id", "sub"):
        value = data.get(key)
        try:
            return int(value)
        except (TypeError, ValueError):
            continue
    return 1


class PushService:
    def __init__(self) -> None:
        self.messages: List[Dict[str, Any]] = []
        self.tasks: Dict[str, Dict[str, Any]] = {}
        self.push_configs: List[Dict[str, Any]] = []
        self.email_configs: List[Dict[str, Any]] = []
        self._next_body_id = 1
        self._next_config_id = 1
        self._subscribers: Dict[Any, Dict[str, Any]] = {}

    def add_subscriber(self, websocket: Any, subscription_id: str = "messages", user_id: Optional[int] = None) -> None:
        self._subscribers[websocket] = {"subscription_id": subscription_id, "user_id": user_id}

    def remove_subscriber(self, websocket: Any) -> None:
        self._subscribers.pop(websocket, None)

    async def broadcast(self, message: Dict[str, Any]) -> None:
        if not self._subscribers:
            return
        rec_user_id = self._int_or_none(message.get("recUserId"))
        stale = []
        for websocket, subscriber in list(self._subscribers.items()):
            subscriber_user_id = self._int_or_none(subscriber.get("user_id"))
            if rec_user_id is not None and subscriber_user_id is not None and rec_user_id != subscriber_user_id:
                continue
            try:
                subscription_id = str(subscriber.get("subscription_id") or "messages")
                frame = self._message_frame("/user/queue/messages", message, subscription_id)
                await websocket.send_text(frame)
            except Exception:
                stale.append(websocket)
        for websocket in stale:
            self.remove_subscriber(websocket)

    def page_messages(self, body: Optional[Dict[str, Any]], current_user_id: Optional[int] = None) -> Dict[str, Any]:
        payload = body or {}
        page_form = payload.get("pageForm") or {}
        rows = list(self.messages)
        if current_user_id is not None:
            rows = [row for row in rows if int(row.get("recUserId") or 0) == current_user_id]
        for key in ("recUserId", "readFlag", "type", "pushWay", "pushStatus"):
            if key in payload and payload.get(key) is not None and payload.get(key) != "":
                rows = [row for row in rows if row.get(key) == payload.get(key)]
        source_type = payload.get("sourceType")
        if source_type == "system":
            rows = [row for row in rows if row.get("sysMsg") is True or not row.get("sendUserId")]
        if source_type == "user":
            rows = [row for row in rows if row.get("sysMsg") is False and row.get("sendUserId")]
        keyword = str(payload.get("keyword") or page_form.get("keyword") or "").strip().lower()
        if keyword:
            rows = [row for row in rows if keyword in json.dumps(row, ensure_ascii=False).lower()]
        rows.sort(key=lambda row: str(row.get("createTime") or ""), reverse=True)
        curr_page = max(int(page_form.get("currPage") or 1), 1)
        page_size = max(int(page_form.get("pageSize") or 20), 1)
        start = (curr_page - 1) * page_size
        return page_result(rows[start : start + page_size], len(rows), curr_page, page_size)

    def unread_count(self, user_id: int) -> int:
        return sum(1 for row in self.messages if int(row.get("recUserId") or 0) == user_id and not row.get("readFlag"))

    def mark_read(self, ids: Iterable[str], read_flag: bool = True) -> None:
        id_set = {str(item) for item in ids}
        for row in self.messages:
            if str(row.get("msgId")) in id_set:
                row["readFlag"] = read_flag

    def mark_all_read(self, user_id: int) -> None:
        for row in self.messages:
            if int(row.get("recUserId") or 0) == user_id:
                row["readFlag"] = True

    def delete_messages(self, ids: Iterable[str]) -> None:
        id_set = {str(item) for item in ids}
        self.messages = [row for row in self.messages if str(row.get("msgId")) not in id_set]

    async def send_test(self, request: Dict[str, Any], current_user_id: int) -> Dict[str, Any]:
        task_id = uuid.uuid4().hex
        users = self._resolve_users(request, current_user_id)
        requested = max(int(request.get("messageCount") or 1), 1)
        status = self._new_task(task_id, requested, len(users))
        show_in_center = self._show_in_message_center(request, True)
        count = requested if requested > 1 else len(users)
        targets = users if requested <= len(users) else [users[index % len(users)] for index in range(count)]
        for index, rec_user_id in enumerate(targets, start=1):
            message = self._build_message(request, rec_user_id, current_user_id, task_id, index, show_in_center)
            if show_in_center:
                self.messages.insert(0, message)
            status["sentCount"] += 1
            await self.broadcast(message)
        status["status"] = "FINISHED"
        status["finishedAt"] = current_millis()
        return dict(status)

    async def start_perf(self, request: Dict[str, Any], current_user_id: int) -> Dict[str, Any]:
        request = dict(request)
        request["showInMessageCenter"] = bool(request.get("showInMessageCenter", False))
        task_id = uuid.uuid4().hex
        users = self._resolve_users(request, current_user_id)
        requested = max(int(request.get("messageCount") or 1), 1)
        status = self._new_task(task_id, requested, len(users))
        asyncio.create_task(self._run_perf_task(task_id, request, current_user_id, users))
        return self.get_task(task_id) or dict(status)

    async def _run_perf_task(
        self,
        task_id: str,
        request: Dict[str, Any],
        current_user_id: int,
        users: List[int],
    ) -> None:
        status = self.tasks[task_id]
        requested = int(status.get("requestedMessages") or 0)
        batch_size = max(int(request.get("batchSize") or 20), 1)
        interval_ms = max(int(request.get("intervalMillis") or 0), 0)
        show_in_center = self._show_in_message_center(request, False)
        try:
            targets = [users[index % len(users)] for index in range(requested)]
            for index, rec_user_id in enumerate(targets, start=1):
                message = self._build_message(request, rec_user_id, current_user_id, task_id, index, show_in_center)
                if show_in_center:
                    self.messages.insert(0, message)
                status["sentCount"] += 1
                await self.broadcast(message)
                if index < requested and index % batch_size == 0 and interval_ms > 0:
                    await asyncio.sleep(interval_ms / 1000)
            status["status"] = "FINISHED"
            status["finishedAt"] = current_millis()
        except Exception as exc:
            status["status"] = "FAILED"
            status["failedCount"] = max(requested - int(status.get("sentCount") or 0), 0)
            status["finishedAt"] = current_millis()
            status["errorMessage"] = str(exc)

    def ack(self, ack: Dict[str, Any]) -> Dict[str, Any]:
        task_id = str(ack.get("testRunId") or "")
        status = self.tasks.get(task_id)
        if status is None:
            status = self._new_task(task_id or uuid.uuid4().hex, 0, 0)
            status["status"] = "FINISHED"
        msg_id = str(ack.get("msgId") or "")
        acked_ids = status.setdefault("_acked_ids", set())
        if msg_id and msg_id in acked_ids:
            return self._public_status(status)
        if msg_id:
            acked_ids.add(msg_id)
        status["ackCount"] = int(status.get("ackCount") or 0) + 1
        latency = ack.get("latencyMs")
        if latency is not None:
            values = list(status.get("_latencies") or [])
            values.append(int(latency))
            status["_latencies"] = values
            status["avgLatencyMs"] = sum(values) / len(values)
            status["maxLatencyMs"] = max(values)
        return self._public_status(status)

    def get_task(self, task_id: str) -> Optional[Dict[str, Any]]:
        status = self.tasks.get(task_id)
        if status is None:
            return None
        return self._public_status(status)

    def list_configs(self, rows: List[Dict[str, Any]], body: Optional[Dict[str, Any]]) -> Dict[str, Any]:
        payload = body or {}
        entity = payload.get("entity") or payload
        page_form = payload.get("pageForm") or {}
        filtered = list(rows)
        for key, value in entity.items():
            if key == "pageForm" or value in (None, ""):
                continue
            filtered = [row for row in filtered if row.get(key) == value]
        curr_page = max(int(page_form.get("currPage") or 1), 1)
        page_size = max(int(page_form.get("pageSize") or 20), 1)
        start = (curr_page - 1) * page_size
        return page_result(filtered[start : start + page_size], len(filtered), curr_page, page_size)

    def save_config(self, rows: List[Dict[str, Any]], entity: Dict[str, Any]) -> Dict[str, Any]:
        row = dict(entity or {})
        now = utc_now_iso()
        if not row.get("id"):
            row["id"] = self._next_config_id
            self._next_config_id += 1
            row["createTime"] = now
            rows.append(row)
        else:
            for index, existing in enumerate(rows):
                if existing.get("id") == row.get("id"):
                    row = {**existing, **row}
                    rows[index] = row
                    break
            else:
                rows.append(row)
        row["updateTime"] = now
        return row

    def delete_configs(self, rows: List[Dict[str, Any]], ids: Iterable[Any]) -> bool:
        id_set = {int(item) for item in ids if str(item).isdigit()}
        before = len(rows)
        rows[:] = [row for row in rows if int(row.get("id") or 0) not in id_set]
        return len(rows) != before

    def _resolve_users(self, request: Dict[str, Any], current_user_id: int) -> List[int]:
        raw = request.get("recUserIds") or request.get("recUserId") or []
        if isinstance(raw, str) and "," in raw:
            raw = [item.strip() for item in raw.split(",")]
        elif not isinstance(raw, list):
            raw = [raw]
        users = []
        for value in raw:
            try:
                users.append(int(value))
            except (TypeError, ValueError):
                continue
        return users or [current_user_id]

    def _int_or_none(self, value: Any) -> Optional[int]:
        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    def _build_message(
        self,
        request: Dict[str, Any],
        rec_user_id: int,
        send_user_id: int,
        task_id: str,
        index: int,
        show_in_center: bool,
    ) -> Dict[str, Any]:
        msg_id = uuid.uuid4().hex
        body_id = self._next_body_id
        self._next_body_id += 1
        title = str(request.get("title") or "测试消息")
        content = request.get("content") or "这是一条测试消息"
        if index > 1:
            title = "%s #%s" % (title, index)
        return {
            "msgId": msg_id,
            "msgBodyId": body_id,
            "recUserId": rec_user_id,
            "sendUserId": request.get("sendUserId") or send_user_id,
            "sysMsg": bool(request.get("sysMsg", False)),
            "pushStatus": "success",
            "pushWay": "internal",
            "readFlag": False,
            "content": content,
            "title": title,
            "level": request.get("level") or 0,
            "type": request.get("pushMsgType") or "notification",
            "createTime": utc_now_iso(),
            "url": request.get("url"),
            "extend": {**dict(request.get("extend") or {}), "showInMessageCenter": show_in_center},
            "testRunId": task_id,
            "clientSentAt": current_millis(),
        }

    def _show_in_message_center(self, request: Dict[str, Any], default: bool) -> bool:
        if "showInMessageCenter" in request:
            value = request.get("showInMessageCenter")
        else:
            value = dict(request.get("extend") or {}).get("showInMessageCenter", default)
        if isinstance(value, str):
            return value.lower() in ("1", "true", "yes", "y")
        return bool(value)

    def _new_task(self, task_id: str, requested: int, users: int) -> Dict[str, Any]:
        status = {
            "taskId": task_id,
            "status": "RUNNING",
            "requestedMessages": requested,
            "resolvedUsers": users,
            "startedAt": current_millis(),
            "finishedAt": None,
            "sentCount": 0,
            "failedCount": 0,
            "ackCount": 0,
            "avgLatencyMs": 0,
            "maxLatencyMs": 0,
            "errorMessage": None,
        }
        self.tasks[task_id] = status
        return status

    def _public_status(self, status: Dict[str, Any]) -> Dict[str, Any]:
        public = dict(status)
        public.pop("_latencies", None)
        public.pop("_acked_ids", None)
        return public

    def _message_frame(self, destination: str, payload: Dict[str, Any], subscription_id: str) -> str:
        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
        headers = {
            "destination": destination,
            "content-type": "application/json;charset=UTF-8",
            "subscription": subscription_id,
            "message-id": uuid.uuid4().hex,
            "content-length": str(len(body.encode("utf-8"))),
        }
        header_text = "\n".join("%s:%s" % (key, value) for key, value in headers.items())
        return "MESSAGE\n%s\n\n%s\x00" % (header_text, body)
