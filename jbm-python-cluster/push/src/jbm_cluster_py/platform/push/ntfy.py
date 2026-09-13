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
    topic = str(config.get("topic") or "").strip()
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", topic):
        raise ValueError("ntfy 主题须为 1～64 位字母、数字、下划线或短横线")
    priority = config.get("priority", 3)
    if isinstance(priority, bool) or str(priority) not in {"1", "2", "3", "4", "5"}:
        raise ValueError("ntfy 优先级须为 1～5 的整数")
    token = str(config.get("token") or "").strip()
    if "\r" in token or "\n" in token:
        raise ValueError("ntfy Token 不能包含换行")
    return {"serverUrl": server, "topic": topic, "token": token, "priority": int(priority)}


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
    # Topic/priority may vary per message; server and credentials may not.
    extend = payload.get("extend") or {}
    config = validate_config({
        **config,
        "topic": payload.get("topic") or extend.get("topic") or config["topic"],
        "priority": payload.get("priority", extend.get("priority", config["priority"])),
    })
    content = payload.get("content")
    if content is None or content == "":
        raise ValueError("ntfy 消息内容不能为空")
    message = {
        "topic": config["topic"],
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
    return {"provider": "ntfy", "topic": config["topic"], "messageId": result["id"]}
