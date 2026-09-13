"""ntfy transport; connection credentials come only from channel configuration."""
import json
import re
from typing import Any, Mapping
from urllib.parse import urlsplit

import httpx

CHANNEL_TYPE = 8
MASKED_TOKEN = "********"


def validate_config(value: Any) -> dict[str, Any]:
    try:
        config = dict(value) if isinstance(value, Mapping) else json.loads(value or "{}")
        if not isinstance(config, dict):
            raise ValueError
    except (ValueError, TypeError):
        raise ValueError("ntfy 配置必须是 JSON 对象") from None
    server = str(config.get("serverUrl") or "").strip().rstrip("/")
    try:
        url = urlsplit(server)
        valid = url.scheme in {"http", "https"} and url.hostname and url.port != 0
    except ValueError:
        valid = False
    if not valid or url.username or url.password or url.query or url.fragment:
        raise ValueError("ntfy 服务地址必须是 HTTP/HTTPS 地址，不能包含账号、查询参数或片段")
    prefix = str(config.get("topicPrefix") or "jbm").strip()
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,32}", prefix):
        raise ValueError("ntfy 主题前缀须为 1～32 位字母、数字、下划线或短横线")
    priority = config.get("priority", 3)
    if isinstance(priority, bool) or str(priority) not in {"1", "2", "3", "4", "5"}:
        raise ValueError("ntfy 优先级须为 1～5 的整数")
    token = str(config.get("token") or "").strip()
    if "\r" in token or "\n" in token:
        raise ValueError("ntfy Token 不能包含换行")
    return {"serverUrl": server, "topicPrefix": prefix, "token": token, "priority": int(priority)}


def recipient_topic(config: Mapping[str, Any], user_id: Any) -> str:
    """Use JBM's resolved recipient; zero is the standard broadcast recipient."""
    if isinstance(user_id, bool) or not re.fullmatch(r"[0-9]{1,19}", str(user_id)):
        raise ValueError("ntfy 需要有效的 JBM 接收用户 recUserId")
    recipient = int(user_id)
    if recipient > 9223372036854775807:
        raise ValueError("ntfy 接收用户 ID 超出范围")
    prefix = validate_config(config)["topicPrefix"]
    return f"{prefix}-user-{recipient}" if recipient else f"{prefix}-broadcast"


def public_config(row: dict[str, Any]) -> dict[str, Any]:
    if row.get("type") != CHANNEL_TYPE:
        return row
    result = dict(row)
    config = json.loads(result.get("releaseContent") or "{}")
    if config.get("token"):
        config["token"] = MASKED_TOKEN
    result["releaseContent"] = json.dumps(config, ensure_ascii=False)
    return result


async def send(config: Mapping[str, Any], payload: Mapping[str, Any]) -> dict[str, Any]:
    config = validate_config(config)
    extend = payload.get("extend") or {}
    if payload.get("topic") or extend.get("topic"):
        raise ValueError("ntfy 主题由 JBM 收件人决定，请使用 recUserId / recUserIds")
    topic = recipient_topic(config, payload.get("recUserId"))
    config = validate_config({
        **config,
        "priority": payload.get("priority", extend.get("priority", config["priority"])),
    })
    content = payload.get("content")
    if content is None or content == "":
        raise ValueError("ntfy 消息内容不能为空")
    message = {
        "topic": topic,
        "title": str(payload.get("title") or "JBM 通知"),
        "message": content if isinstance(content, str) else json.dumps(content, ensure_ascii=False),
        "priority": config["priority"],
    }
    headers = {"Authorization": "Bearer " + config["token"]} if config["token"] else {}
    async with httpx.AsyncClient(timeout=10, follow_redirects=False, trust_env=False) as client:
        response = await client.post(config["serverUrl"], json=message, headers=headers)
    if response.status_code != 200:
        # Do not persist remote response bodies, URLs, or authentication headers.
        raise ValueError(f"ntfy 发送失败（HTTP {response.status_code}）")
    result = response.json()
    if not isinstance(result, dict) or not result.get("id") or result.get("event") != "message":
        raise ValueError("ntfy 未返回有效的消息回执")
    return {"provider": "ntfy", "topic": topic, "messageId": result["id"]}
