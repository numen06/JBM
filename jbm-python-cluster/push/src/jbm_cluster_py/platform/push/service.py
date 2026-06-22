from __future__ import annotations

import base64
import asyncio
import hashlib
import hmac
import json
import logging
import smtplib
import time
import uuid
from datetime import datetime, timezone
from email.message import EmailMessage
from typing import Any, Dict, Iterable, List, Mapping, Optional
from urllib.parse import quote

import httpx
from jbm_cluster_py.common.masterdata import model_dump_compat
from jbm_cluster_py.common.result import page_result
from jbm_cluster_py.platform.push.push_events import PushMessageEvent
from jbm_cluster_py.platform.push.push_message_repository import PushMessageRepository

logger = logging.getLogger(__name__)


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
    def __init__(
        self,
        rabbitmq: Any = None,
        rabbitmq_config: Optional[Mapping[str, Any]] = None,
        message_repository: Optional[PushMessageRepository] = None,
        config_repository: Any = None,
        sms_config: Optional[Mapping[str, Any]] = None,
        email_config: Optional[Mapping[str, Any]] = None,
        push_config: Optional[Mapping[str, Any]] = None,
    ) -> None:
        self.rabbitmq = rabbitmq
        self.rabbitmq_config = dict(rabbitmq_config or {})
        self.message_repository = message_repository
        self.config_repository = config_repository
        self.sms_config = dict(sms_config or {})
        self.email_config = dict(email_config or {})
        self.push_config = dict(push_config or {})
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

    async def page_messages(self, body: Optional[Dict[str, Any]], current_user_id: Optional[int] = None) -> Dict[str, Any]:
        if self.message_repository is not None:
            return await self.message_repository.page_messages(body, current_user_id)
        payload = body or {}
        page_form = payload.get("pageForm") or {}
        rows = list(self.messages)
        if current_user_id is not None:
            rows = [
                row
                for row in rows
                if int(row.get("recUserId") or 0) in {current_user_id, 0}
            ]
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

    async def unread_count(self, user_id: int) -> int:
        if self.message_repository is not None:
            return await self.message_repository.count_unread(user_id)
        return sum(
            1
            for row in self.messages
            if int(row.get("recUserId") or 0) in {user_id, 0} and not row.get("readFlag")
        )

    async def mark_read(self, ids: Iterable[str], read_flag: bool = True) -> None:
        if self.message_repository is not None:
            await self.message_repository.update_read_flag([str(item) for item in ids], read_flag)
            return
        id_set = {str(item) for item in ids}
        for row in self.messages:
            if str(row.get("msgId")) in id_set:
                row["readFlag"] = read_flag

    async def mark_all_read(self, user_id: int) -> None:
        if self.message_repository is not None:
            await self.message_repository.mark_all_read(user_id)
            return
        for row in self.messages:
            if int(row.get("recUserId") or 0) in {user_id, 0}:
                row["readFlag"] = True

    async def delete_messages(self, ids: Iterable[str]) -> None:
        if self.message_repository is not None:
            await self.message_repository.delete_by_ids([str(item) for item in ids])
            return
        id_set = {str(item) for item in ids}
        self.messages = [row for row in self.messages if str(row.get("msgId")) not in id_set]

    def push_queue_name(self) -> str:
        return str(
            self.rabbitmq_config.get("push-message-queue") or "pushMessage-in-0.jbm-cluster-platform-push"
        )

    def _rabbitmq_ready(self) -> bool:
        return self.rabbitmq is not None and bool(getattr(self.rabbitmq, "enabled", False))

    async def publish_message(self, request: Dict[str, Any], current_user_id: int) -> Dict[str, Any]:
        users = self._resolve_users(request, current_user_id)
        sync_delivery = bool(request.get("syncDelivery") or request.get("sync_delivery"))
        sent = 0
        deliveries: list[Dict[str, Any]] = []
        for rec_user_id in users:
            event = self._build_event(request, rec_user_id, current_user_id)
            delivery = await self._publish_or_deliver(event, sync_delivery)
            if delivery:
                deliveries.append(delivery)
            sent += 1
        result: Dict[str, Any] = {"sent": sent}
        if deliveries:
            result["deliveries"] = deliveries
            result["deliveryStatus"] = deliveries[0].get("deliveryStatus")
        return result

    async def _publish_or_deliver(self, event: Dict[str, Any], sync_delivery: bool = False) -> Optional[Dict[str, Any]]:
        if self._rabbitmq_ready() and not sync_delivery:
            await self.rabbitmq.publish_json(self.push_queue_name(), event)
            return {"deliveryStatus": "queued", "message": "rabbitmq queued"}
        return await self.handle_push_event(event)

    async def handle_push_event(self, payload: Mapping[str, Any]) -> Optional[Dict[str, Any]]:
        push_way = str(payload.get("pushWay") or payload.get("push_way") or "internal").lower()
        if push_way == "internal":
            await self._deliver_websocket(payload)
            return {"deliveryStatus": "sent", "deliveryChannel": "internal"}
        elif push_way == "sms":
            return await self._deliver_sms(payload)
        elif push_way == "email":
            await self._deliver_email(payload)
            return {"deliveryStatus": "sent", "deliveryChannel": "email"}
        elif push_way == "mqtt":
            await self._deliver_mqtt(payload)
            return {"deliveryStatus": "sent", "deliveryChannel": "mqtt"}
        elif push_way in {"wechat", "miniapp", "app"}:
            await self._record_external_delivery(
                payload,
                push_way,
                error_message="%s真实发送器未接入" % push_way,
            )
            return {
                "deliveryStatus": "failed",
                "deliveryChannel": push_way,
                "errorMessage": "%s真实发送器未接入" % push_way,
            }
        else:
            logger.warning("Unknown pushWay: %s", push_way)
            return {"deliveryStatus": "failed", "deliveryChannel": push_way, "errorMessage": "Unknown pushWay"}

    async def _deliver_websocket(self, payload: Mapping[str, Any]) -> None:
        rec_user_id = self._event_rec_user_id(payload)
        if rec_user_id is None:
            logger.warning("Push event missing recUserId: %s", payload)
            return
        send_user_id = self._int_or_none(payload.get("sendUserId")) or rec_user_id
        task_id = str(payload.get("testRunId") or payload.get("taskId") or uuid.uuid4().hex)
        index = max(int(payload.get("messageIndex") or payload.get("index") or 1), 1)
        show_in_center = self._show_in_message_center(dict(payload), True)
        request = dict(payload)
        message = self._build_message(request, rec_user_id, send_user_id, task_id, index, show_in_center)
        push_way = str(payload.get("pushWay") or payload.get("push_way") or "internal")
        message["pushWay"] = push_way
        if show_in_center:
            await self._persist_message(message)
        await self.broadcast(message)

    async def _deliver_sms(self, payload: Mapping[str, Any]) -> Dict[str, Any]:
        try:
            sms_config = await self._effective_sms_config()
            if not sms_config:
                raise ValueError("短信通知通道未启用")
            if self._sms_dry_run(sms_config):
                await self._record_external_delivery(
                    payload,
                    "sms",
                    status="issued",
                    delivery_status="dry-run",
                    detail={"message": "jaja7 dry-run: SMS not sent"},
                )
                return {
                    "deliveryStatus": "dry-run",
                    "deliveryChannel": "sms",
                    "message": "jaja7 dry-run: SMS not sent",
                }
            result = await self._send_aliyun_sms(payload, sms_config)
            await self._record_external_delivery(
                payload,
                "sms",
                status="issued",
                delivery_status="sent",
                detail={"provider": "aliyun", "requestId": result.get("RequestId")},
            )
            return {"deliveryStatus": "sent", "deliveryChannel": "sms", "provider": "aliyun", "requestId": result.get("RequestId")}
        except Exception as exc:
            logger.warning("SMS delivery failed: %s", exc)
            await self._record_external_delivery(payload, "sms", error_message=str(exc))
            return {"deliveryStatus": "failed", "deliveryChannel": "sms", "errorMessage": str(exc)}

    async def _deliver_email(self, payload: Mapping[str, Any]) -> None:
        try:
            email_config = await self._effective_email_config()
            if not email_config:
                raise ValueError("邮件通知通道未启用")
            result = await self._send_smtp_email(payload, email_config)
            await self._record_external_delivery(
                payload,
                "email",
                status="issued",
                delivery_status="sent",
                detail=result,
            )
        except Exception as exc:
            logger.warning("Email delivery failed: %s", exc)
            await self._record_external_delivery(payload, "email", error_message=str(exc))

    async def _deliver_mqtt(self, payload: Mapping[str, Any]) -> None:
        await self._record_external_delivery(payload, "mqtt")

    async def _record_external_delivery(
        self,
        payload: Mapping[str, Any],
        channel: str,
        status: str = "fail",
        delivery_status: str = "failed",
        error_message: Optional[str] = None,
        detail: Optional[Mapping[str, Any]] = None,
    ) -> None:
        rec_user_id = self._event_rec_user_id(payload) or self._int_or_none(payload.get("sendUserId")) or 0
        send_user_id = self._int_or_none(payload.get("sendUserId")) or rec_user_id
        task_id = str(payload.get("testRunId") or payload.get("taskId") or uuid.uuid4().hex)
        show_in_center = self._show_in_message_center(dict(payload), True)
        message = self._build_message(dict(payload), rec_user_id, send_user_id, task_id, 1, show_in_center)
        message["pushWay"] = channel
        message["pushStatus"] = status
        safe_extend = {
            **dict(message.get("extend") or {}),
            **dict(detail or {}),
            "deliveryStatus": delivery_status,
            "deliveryChannel": channel,
        }
        phone = self._sms_value(payload, "phoneNumber", "phone_number", "phone")
        if phone:
            safe_extend["phoneNumber"] = _mask_phone(phone)
        if error_message:
            safe_extend["errorMessage"] = error_message
        elif status == "fail":
            safe_extend["errorMessage"] = "%s真实发送器未接入" % channel
        message["extend"] = safe_extend
        if show_in_center:
            await self._persist_message(message)
        await self.broadcast(message)

    def _sms_dry_run(self, config: Optional[Mapping[str, Any]] = None) -> bool:
        source = dict(config or {})
        value = source.get("jaja7-dry-run")
        if value is None:
            value = source.get("jaja7DryRun")
        if value is None:
            value = self.push_config.get("jaja7-dry-run")
        if value is None:
            value = self.push_config.get("jaja7DryRun")
        if isinstance(value, str):
            return value.lower() in ("1", "true", "yes", "y")
        return bool(value)

    async def _send_aliyun_sms(self, payload: Mapping[str, Any], config: Mapping[str, Any]) -> Dict[str, Any]:
        access_key_id = self._sms_config_value(config, "accessKeyId", "access-key-id", "access_key_id")
        access_key_secret = self._sms_config_value(config, "accessKeySecret", "access-key-secret", "access_key_secret")
        phone_number = self._sms_value(payload, "phoneNumber", "phone_number", "phone")
        template_code = self._sms_value(payload, "templateCode", "template_code") or self._sms_config_value(
            config,
            "pinTemplateCode",
            "pin-template-code",
            "templateCode",
            "template-code",
        )
        sign_name = self._sms_value(payload, "signName", "sign_name") or self._sms_config_value(
            config,
            "signName",
            "sign-name",
        )
        if not access_key_id or not access_key_secret:
            raise ValueError("短信通道缺少 aliyun.sms.accessKeyId/accessKeySecret 配置")
        if not phone_number:
            raise ValueError("短信通知缺少 phoneNumber")
        if not template_code:
            raise ValueError("短信通知缺少 templateCode")
        if not sign_name:
            raise ValueError("短信通道缺少 signName")
        template_param = payload.get("params") or payload.get("templateParam") or payload.get("template_param") or {}
        if not isinstance(template_param, Mapping):
            template_param = {}
        params = {
            "AccessKeyId": access_key_id,
            "Action": "SendSms",
            "Format": "JSON",
            "PhoneNumbers": str(phone_number),
            "RegionId": str(self._sms_config_value(config, "regionId", "region-id") or "cn-hangzhou"),
            "SignName": str(sign_name),
            "SignatureMethod": "HMAC-SHA1",
            "SignatureNonce": uuid.uuid4().hex,
            "SignatureVersion": "1.0",
            "TemplateCode": str(template_code),
            "TemplateParam": json.dumps(dict(template_param), ensure_ascii=False, separators=(",", ":")),
            "Timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "Version": "2017-05-25",
        }
        params["Signature"] = _aliyun_signature(params, str(access_key_secret))
        async with httpx.AsyncClient(timeout=httpx.Timeout(10.0, connect=5.0), trust_env=False) as client:
            response = await client.post("https://dysmsapi.aliyuncs.com/", data=params)
        response.raise_for_status()
        result = response.json()
        if str(result.get("Code") or "").upper() != "OK":
            raise ValueError(str(result.get("Message") or result.get("Code") or "发送短信错误"))
        return dict(result)

    async def _effective_sms_config(self) -> Dict[str, Any]:
        fallback = {
            **dict(self.sms_config or {}),
            **{key: value for key, value in self.push_config.items() if key in {"jaja7-dry-run", "jaja7DryRun"}},
        }
        if self.config_repository is None:
            return fallback
        try:
            rows = await self.config_repository.list_push_configs({"type": 3})
        except Exception as exc:
            logger.warning("Failed to read sms channel config: %s", exc)
            return fallback
        if not rows:
            return fallback
        enabled_rows = [row for row in rows if row.get("enable") is not False]
        if not enabled_rows:
            return {}
        row = enabled_rows[0]
        content = row.get("releaseContent") or row.get("release_content") or ""
        parsed: Dict[str, Any] = {}
        if isinstance(content, Mapping):
            parsed = dict(content)
        elif str(content).strip():
            try:
                loaded = json.loads(str(content))
                if isinstance(loaded, Mapping):
                    parsed = dict(loaded)
            except json.JSONDecodeError:
                logger.warning("SMS channel releaseContent is not valid JSON")
        return {**fallback, **parsed}

    def _sms_config_value(self, config: Mapping[str, Any], *keys: str) -> Optional[str]:
        for key in keys:
            value = config.get(key)
            if value not in (None, ""):
                return str(value)
        return None

    async def _effective_email_config(self) -> Dict[str, Any]:
        fallback = dict(self.email_config or {})
        if self.config_repository is None:
            return fallback
        channel_config: Dict[str, Any] = {}
        try:
            rows = await self.config_repository.list_push_configs({"type": 2})
        except Exception as exc:
            logger.warning("Failed to read email channel config: %s", exc)
            rows = []
        if rows:
            enabled_rows = [row for row in rows if row.get("enable") is not False]
            if not enabled_rows:
                return {}
            channel_config = self._parse_release_content(enabled_rows[0].get("releaseContent") or "")
        try:
            email_rows = await self.config_repository.list_email_configs({})
        except Exception as exc:
            logger.warning("Failed to read smtp email config: %s", exc)
            email_rows = []
        email_row = next((row for row in email_rows if row.get("host")), {})
        return {**fallback, **channel_config, **dict(email_row or {})}

    async def _send_smtp_email(self, payload: Mapping[str, Any], config: Mapping[str, Any]) -> Dict[str, Any]:
        recipient = self._email_value(payload, "receiver", "mailTo", "to", "email")
        subject = str(payload.get("title") or "JBM 通知")
        content = str(payload.get("content") or "")
        if not recipient:
            raise ValueError("邮件通知缺少 receiver/mailTo")
        await asyncio.to_thread(self._send_smtp_email_sync, recipient, subject, content, dict(config), dict(payload))
        return {"provider": "smtp", "host": str(config.get("host") or "")}

    def _send_smtp_email_sync(
        self,
        recipient: str,
        subject: str,
        content: str,
        config: Mapping[str, Any],
        payload: Mapping[str, Any],
    ) -> None:
        host = self._email_config_value(config, "host")
        port = int(self._email_config_value(config, "port") or 465)
        username = self._email_config_value(config, "username", "user")
        password = self._email_config_value(config, "password")
        sender = self._email_config_value(config, "from", "fromAddress", "from-address") or username
        if not host:
            raise ValueError("邮件通道缺少 spring.mail.host 配置")
        if not sender:
            raise ValueError("邮件通道缺少发件人配置")
        message = EmailMessage()
        message["Subject"] = subject
        message["From"] = sender
        message["To"] = recipient
        content_type = str(payload.get("contentType") or payload.get("content_type") or "").lower()
        if content_type == "html":
            message.set_content(content, subtype="html")
        else:
            message.set_content(content)
        use_ssl = self._email_bool(config, "ssl", "sslEnable", "ssl-enable", "properties.mail.smtp.ssl.enable")
        use_starttls = self._email_bool(
            config,
            "starttls",
            "starttlsEnable",
            "starttls-enable",
            "properties.mail.smtp.starttls.enable",
        )
        if use_ssl or (not use_starttls and port == 465):
            with smtplib.SMTP_SSL(host, port, timeout=10) as smtp:
                if username and password:
                    smtp.login(username, password)
                smtp.send_message(message)
            return
        with smtplib.SMTP(host, port, timeout=10) as smtp:
            if use_starttls or port == 587:
                smtp.starttls()
            if username and password:
                smtp.login(username, password)
            smtp.send_message(message)

    def _parse_release_content(self, value: Any) -> Dict[str, Any]:
        if isinstance(value, Mapping):
            return dict(value)
        if not str(value or "").strip():
            return {}
        try:
            loaded = json.loads(str(value))
        except json.JSONDecodeError:
            logger.warning("Push channel releaseContent is not valid JSON")
            return {}
        return dict(loaded) if isinstance(loaded, Mapping) else {}

    def _email_config_value(self, config: Mapping[str, Any], *keys: str) -> Optional[str]:
        for key in keys:
            value = config.get(key)
            if value not in (None, ""):
                return str(value)
        return None

    def _email_value(self, payload: Mapping[str, Any], *keys: str) -> Optional[str]:
        for key in keys:
            value = payload.get(key)
            if value not in (None, ""):
                return str(value)
        extend = payload.get("extend")
        if isinstance(extend, Mapping):
            for key in keys:
                value = extend.get(key)
                if value not in (None, ""):
                    return str(value)
        return None

    def _email_bool(self, config: Mapping[str, Any], *keys: str) -> bool:
        value: Any = None
        for key in keys:
            if key in config:
                value = config.get(key)
                break
        if isinstance(value, bool):
            return value
        return str(value or "").strip().lower() in {"1", "true", "yes", "y", "on"}

    def _sms_value(self, payload: Mapping[str, Any], *keys: str) -> Optional[str]:
        for key in keys:
            value = payload.get(key)
            if value not in (None, ""):
                return str(value)
        extend = payload.get("extend")
        if isinstance(extend, Mapping):
            for key in keys:
                value = extend.get(key)
                if value not in (None, ""):
                    return str(value)
        return None

    async def _channel_configured(self, channel: str) -> bool:
        if channel == "sms":
            sms_config = await self._effective_sms_config()
            return bool(
                sms_config
                and (
                    self._sms_dry_run(sms_config)
                    or (
                        self._sms_config_value(sms_config, "accessKeyId", "access-key-id", "access_key_id")
                        and self._sms_config_value(sms_config, "accessKeySecret", "access-key-secret", "access_key_secret")
                    )
                )
            )
        if self.config_repository is None:
            return False
        try:
            if channel == "email":
                rows = await self.config_repository.list_email_configs({})
                return any(row.get("host") for row in rows)
            rows = await self.config_repository.list_push_configs({"enable": True})
            return bool(rows)
        except Exception as exc:
            logger.warning("Failed to read %s push channel config: %s", channel, exc)
            return False

    def _event_rec_user_id(self, payload: Mapping[str, Any]) -> Optional[int]:
        rec_user_id = self._int_or_none(payload.get("recUserId"))
        if rec_user_id is not None:
            return rec_user_id
        raw_ids = payload.get("recUserIds") or payload.get("rec_user_ids") or []
        if isinstance(raw_ids, list) and raw_ids:
            return self._int_or_none(raw_ids[0])
        return None

    def _build_event(self, request: Dict[str, Any], rec_user_id: int, current_user_id: int) -> Dict[str, Any]:
        event = PushMessageEvent(
            eventType=str(request.get("eventType") or "PUSH_MESSAGE"),
            pushWay=str(request.get("pushWay") or "internal"),
            recUserId=rec_user_id,
            sendUserId=self._int_or_none(request.get("sendUserId")) or current_user_id,
            sysMsg=bool(request.get("sysMsg", False)),
            title=request.get("title"),
            content=request.get("content"),
            level=int(request.get("level") or 0),
            pushMsgType=str(request.get("pushMsgType") or "notification"),
            url=request.get("url"),
            extend=dict(request.get("extend") or {}),
            showInMessageCenter=self._show_in_message_center(request, True),
            testRunId=request.get("testRunId"),
            messageIndex=int(request.get("messageIndex") or request.get("index") or 1),
        )
        event_data = model_dump_compat(event)
        for key in (
            "phoneNumber",
            "templateCode",
            "params",
            "signName",
            "templateParam",
            "receiver",
            "mailTo",
            "topic",
            "body",
            "qos",
        ):
            if key in request:
                event_data[key] = request[key]
        return event_data

    async def send_test(self, request: Dict[str, Any], current_user_id: int) -> Dict[str, Any]:
        task_id = uuid.uuid4().hex
        users = self._resolve_users(request, current_user_id)
        requested = max(int(request.get("messageCount") or 1), 1)
        status = self._new_task(task_id, requested, len(users))
        show_in_center = self._show_in_message_center(request, True)
        count = requested if requested > 1 else len(users)
        targets = users if requested <= len(users) else [users[index % len(users)] for index in range(count)]
        publish_request = dict(request)
        publish_request["testRunId"] = task_id
        for index, rec_user_id in enumerate(targets, start=1):
            single_request = dict(publish_request)
            single_request["recUserId"] = rec_user_id
            single_request["messageIndex"] = index
            if not show_in_center:
                single_request["showInMessageCenter"] = False
            event = self._build_event(single_request, rec_user_id, current_user_id)
            await self._publish_or_deliver(event)
            status["sentCount"] += 1
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
                    await self._persist_message(message)
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

    async def list_push_configs(self, body: Optional[Dict[str, Any]]) -> Dict[str, Any]:
        if self.config_repository is not None:
            return await self.config_repository.page_push_configs(body)
        return self.list_configs(self.push_configs, body)

    async def list_push_config_rows(self, body: Optional[Dict[str, Any]]) -> list[Dict[str, Any]]:
        if self.config_repository is not None:
            return await self.config_repository.list_push_configs(body)
        return self.list_configs(self.push_configs, body)["contents"]

    async def save_push_config(self, body: Dict[str, Any]) -> Dict[str, Any]:
        if self.config_repository is not None:
            return await self.config_repository.save_push_config(body)
        return self.save_config(self.push_configs, body)

    async def delete_push_configs(self, ids: Iterable[Any]) -> bool:
        if self.config_repository is not None:
            return await self.config_repository.delete_push_configs(ids)
        return self.delete_configs(self.push_configs, ids)

    async def list_email_configs(self, body: Optional[Dict[str, Any]]) -> Dict[str, Any]:
        if self.config_repository is not None:
            return await self.config_repository.page_email_configs(body)
        return self.list_configs(self.email_configs, body)

    async def list_email_config_rows(self, body: Optional[Dict[str, Any]]) -> list[Dict[str, Any]]:
        if self.config_repository is not None:
            return await self.config_repository.list_email_configs(body)
        return self.list_configs(self.email_configs, body)["contents"]

    async def save_email_config(self, body: Dict[str, Any]) -> Dict[str, Any]:
        if self.config_repository is not None:
            return await self.config_repository.save_email_config(body)
        return self.save_config(self.email_configs, body)

    async def delete_email_configs(self, ids: Iterable[Any]) -> bool:
        if self.config_repository is not None:
            return await self.config_repository.delete_email_configs(ids)
        return self.delete_configs(self.email_configs, ids)

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
        push_way = str(request.get("pushWay") or "internal")
        return {
            "msgId": msg_id,
            "msgBodyId": body_id,
            "recUserId": rec_user_id,
            "sendUserId": request.get("sendUserId") or send_user_id,
            "sysMsg": bool(request.get("sysMsg", False)),
            "pushStatus": "issued",
            "pushWay": push_way,
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

    async def _persist_message(self, message: Dict[str, Any]) -> Dict[str, Any]:
        if self.message_repository is not None:
            return await self.message_repository.save_message(message)
        self.messages.insert(0, message)
        return message

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


def _percent_encode(value: Any) -> str:
    return quote(str(value), safe="~")


def _aliyun_signature(params: Mapping[str, Any], access_key_secret: str) -> str:
    canonicalized = "&".join(
        "%s=%s" % (_percent_encode(key), _percent_encode(params[key]))
        for key in sorted(params)
    )
    string_to_sign = "POST&%2F&" + _percent_encode(canonicalized)
    digest = hmac.new(
        (access_key_secret + "&").encode("utf-8"),
        string_to_sign.encode("utf-8"),
        hashlib.sha1,
    ).digest()
    return base64.b64encode(digest).decode("ascii")


def _mask_phone(phone: str) -> str:
    value = str(phone or "")
    if len(value) < 7:
        return value
    return value[:3] + "****" + value[-4:]
