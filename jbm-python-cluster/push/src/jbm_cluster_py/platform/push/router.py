from __future__ import annotations

import logging
from typing import Any, Dict, Optional

from fastapi import APIRouter, Body, Header, WebSocket, WebSocketDisconnect

from jbm_cluster_py.common.result import fail, ok
from jbm_cluster_py.platform.push.business_events import BusinessEventService
from jbm_cluster_py.platform.push.service import PushService, parse_user_id

logger = logging.getLogger(__name__)


def _ids(body: Optional[Dict[str, Any]]) -> list[Any]:
    if not body:
        return []
    value = body.get("ids") or body.get("msgIds") or []
    return value if isinstance(value, list) else [value]


def build_push_router(service: PushService, business_events: Optional[BusinessEventService] = None) -> APIRouter:
    router = APIRouter()

    @router.websocket("/ws")
    async def websocket_endpoint(websocket: WebSocket) -> None:
        subprotocol = _select_stomp_subprotocol(websocket.headers.get("sec-websocket-protocol"))
        await websocket.accept(subprotocol=subprotocol)
        current_user_id: Optional[int] = None
        try:
            while True:
                frame = await websocket.receive_text()
                text = frame.replace("\x00", "").strip()
                if not text:
                    continue
                command = text.split("\n", 1)[0].strip().upper()
                if command in {"CONNECT", "STOMP"}:
                    current_user_id = parse_user_id(_stomp_headers(text).get("Authorization"))
                    await websocket.send_text("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\x00")
                elif command == "SUBSCRIBE":
                    service.add_subscriber(websocket, _stomp_headers(text).get("id", "messages"), current_user_id)
                elif command == "UNSUBSCRIBE":
                    service.remove_subscriber(websocket)
                elif command == "DISCONNECT":
                    break
                else:
                    logger.debug("Ignoring unsupported STOMP frame command: %s", command)
        except WebSocketDisconnect:
            pass
        except Exception as exc:
            logger.warning("Push websocket closed by error: %s", exc)
        finally:
            service.remove_subscriber(websocket)
            try:
                await websocket.close()
            except Exception:
                pass

    @router.post("/pushMessage/read")
    async def read_messages(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        service.mark_read(_ids(body), True)
        return ok("标记已读成功", "标记已读成功")

    @router.post("/pushMessage/readAllCurr")
    async def read_all_curr(authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        service.mark_all_read(parse_user_id(authorization))
        return ok("全部标记已读成功", "全部标记已读成功")

    @router.post("/pushMessage/unread")
    async def unread_messages(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        service.mark_read(_ids(body), False)
        return ok("标记未读成功", "标记未读成功")

    @router.post("/pushMessage/pageList")
    async def page_list(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.page_messages(body), "查询消息列表成功")

    @router.post("/pushMessage/findCurrMessagePage")
    async def current_page(
        body: Optional[Dict[str, Any]] = Body(default=None),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        return ok(service.page_messages(body, parse_user_id(authorization)), "获取登录人的消息列表成功")

    @router.post("/pushMessage/unreadCount")
    async def unread_count(authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        return ok(service.unread_count(parse_user_id(authorization)), "获取未读消息数成功")

    @router.post("/pushMessage/deleteByIds")
    async def delete_by_ids(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        service.delete_messages(_ids(body))
        return ok(True, "删除站内信成功")

    @router.post("/pushMessage/sendUserMessage")
    @router.post("/pushMessage/sendSysMessage")
    @router.post("/pushMessage/sendPushMsg")
    async def send_push_msg(
        body: Optional[Dict[str, Any]] = Body(default=None),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        await service.send_test(body or {}, parse_user_id(authorization))
        return ok("发送推送消息成功", "发送推送消息成功")

    @router.post("/pushMessage/testSend")
    async def test_send(authorization: Optional[str] = Header(default=None)) -> Dict[str, Any]:
        await service.send_test({"title": "管理员测试信息", "content": "Python push test"}, parse_user_id(authorization))
        return ok(True)

    @router.post("/pushTest/send")
    async def push_test_send(
        body: Dict[str, Any] = Body(default_factory=dict),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        return ok(await service.send_test(body, parse_user_id(authorization)), "测试消息已发送")

    @router.post("/pushTest/perf")
    async def push_test_perf(
        body: Dict[str, Any] = Body(default_factory=dict),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        return ok(await service.start_perf(body, parse_user_id(authorization)), "轻压测任务已启动")

    @router.get("/pushTest/perf/{task_id}")
    async def push_test_status(task_id: str) -> Dict[str, Any]:
        status = service.get_task(task_id)
        if status is None:
            return fail(None, "测试任务不存在", 404)
        return ok(status, "查询测试任务成功")

    @router.post("/pushTest/ack")
    async def push_test_ack(body: Dict[str, Any] = Body(default_factory=dict)) -> Dict[str, Any]:
        return ok(service.ack(body), "ACK成功")

    @router.post("/pushConfigInfo/pageList")
    async def push_config_page(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.list_configs(service.push_configs, body), "查询分页列表成功")

    @router.post("/pushConfigInfo/list")
    async def push_config_list(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.list_configs(service.push_configs, body)["contents"], "查询列表成功")

    @router.post("/pushConfigInfo/save")
    async def push_config_save(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.save_config(service.push_configs, body or {}), "保存对象成功")

    @router.post("/pushConfigInfo/deleteByIds")
    async def push_config_delete(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.delete_configs(service.push_configs, _ids(body)), "批量成功删除")

    @router.post("/emailPushConfig/pageList")
    async def email_config_page(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.list_configs(service.email_configs, body), "查询分页列表成功")

    @router.post("/emailPushConfig/list")
    async def email_config_list(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.list_configs(service.email_configs, body)["contents"], "查询列表成功")

    @router.post("/emailPushConfig/model")
    async def email_config_model(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        rows = service.list_configs(service.email_configs, body)["contents"]
        return ok(rows[0] if rows else None, "查询对象成功")

    @router.post("/emailPushConfig/save")
    async def email_config_save(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        entity = (body or {}).get("entity") or body or {}
        return ok(service.save_config(service.email_configs, entity), "保存对象成功")

    @router.post("/emailPushConfig/deleteByIds")
    async def email_config_delete(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.delete_configs(service.email_configs, _ids(body)), "批量成功删除")

    @router.post("/notification/send/sms")
    async def notification_send_sms(
        body: Optional[Dict[str, Any]] = Body(default=None),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        payload = dict(body or {})
        payload.setdefault("pushWay", "sms")
        return ok(await service.publish_message(payload, parse_user_id(authorization)), "发送通知成功")

    @router.post("/notification/send/email")
    async def notification_send_email(
        body: Optional[Dict[str, Any]] = Body(default=None),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        payload = dict(body or {})
        payload.setdefault("pushWay", "email")
        return ok(await service.publish_message(payload, parse_user_id(authorization)), "发送通知成功")

    @router.post("/notification/send/mqtt")
    async def notification_send_mqtt(
        body: Optional[Dict[str, Any]] = Body(default=None),
        authorization: Optional[str] = Header(default=None),
    ) -> Dict[str, Any]:
        payload = dict(body or {})
        payload.setdefault("pushWay", "mqtt")
        return ok(await service.publish_message(payload, parse_user_id(authorization)), "发送通知成功")

    @router.post("/webhookTask/businessEventListener")
    async def business_event_listener(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok("监听测试-1成功", "监听测试-1成功")

    @router.post("/webhookTask/businessEventListener2")
    async def business_event_listener2(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok("监听测试-2成功", "监听测试-2成功")

    @router.get("/webhookTask/run")
    async def webhook_task_run(eventId: str) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.send_business_event_by_event_id(eventId), "触发成功")

    @router.post("/webhookTask/req")
    async def webhook_task_req(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.send_task(body or {}), "请求成功")

    @router.get("/webhookTask/retry")
    async def webhook_task_retry(taskId: str) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.retry_event_task(taskId), "重试成功")

    @router.get("/webhookTask/clear")
    async def webhook_task_clear() -> Dict[str, Any]:
        return ok(False, "清理成功")

    @router.post("/webhookTask/findTask")
    async def webhook_task_find(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.repository.find_task(body or {}), "查询成功")

    @router.post("/webhookTask/selectWebhookTasks")
    async def webhook_task_page(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.repository.page_webhook_tasks(body or {}), "查询成功")

    @router.post("/webhookEventConfig/selectWebhookEventConfigs")
    async def webhook_config_page(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.repository.page_webhook_event_configs(body or {}), "查询成功")

    @router.post("/webhookEventConfig/saveConfig")
    async def webhook_config_save(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        return ok(await business_events.repository.save_config(body or {}), "保存对象成功")

    @router.post("/webhookEventConfig/findConfig")
    async def webhook_config_find(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        if business_events is None:
            return fail(None, "业务事件服务未启用", 503)
        payload = body or {}
        result = None
        if payload.get("eventId"):
            result = await business_events.repository.select_config_by_event_id(str(payload["eventId"]))
        elif payload.get("businessEventCode") and payload.get("url"):
            result = await business_events.repository.select_config_by_code_url(
                str(payload["businessEventCode"]),
                str(payload["url"]),
            )
        return ok(result, "查询成功")

    return router


def _select_stomp_subprotocol(header: Optional[str]) -> Optional[str]:
    if not header:
        return None
    requested = {item.strip() for item in header.split(",")}
    for protocol in ("v12.stomp", "v11.stomp", "v10.stomp"):
        if protocol in requested:
            return protocol
    return None


def _stomp_headers(frame: str) -> Dict[str, str]:
    headers: Dict[str, str] = {}
    for line in frame.split("\n")[1:]:
        if not line:
            break
        key, separator, value = line.partition(":")
        if separator:
            headers[key.strip()] = value.strip()
    return headers
