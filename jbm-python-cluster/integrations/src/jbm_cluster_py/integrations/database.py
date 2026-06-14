from __future__ import annotations

import logging
from typing import Any, Mapping, Optional
from urllib.parse import parse_qsl, quote_plus, urlencode, urlparse

logger = logging.getLogger(__name__)


def parse_properties(content: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in content.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or stripped.startswith("!"):
            continue
        separator = "=" if "=" in stripped else ":"
        if separator not in stripped:
            continue
        key, value = stripped.split(separator, 1)
        result[key.strip()] = value.strip()
    return result


def jdbc_to_async_url(values: Mapping[str, Any]) -> Optional[str]:
    raw_url = str(values.get("spring.datasource.url") or values.get("url") or "").strip()
    username = str(values.get("spring.datasource.username") or values.get("username") or "").strip()
    password = str(values.get("spring.datasource.password") or values.get("password") or "").strip()
    if not raw_url:
        return None
    if raw_url.startswith(("mysql+asyncmy://", "sqlite+aiosqlite://")):
        return raw_url
    if not raw_url.startswith("jdbc:mysql://"):
        logger.warning("Unsupported datasource url for Python services: %s", raw_url)
        return None
    parsed = urlparse(raw_url.replace("jdbc:mysql://", "mysql://", 1))
    jdbc_query = dict(parse_qsl(parsed.query, keep_blank_values=True))
    query: dict[str, str] = {}
    encoding = jdbc_query.get("characterEncoding") or jdbc_query.get("characterencoding")
    if encoding:
        query["charset"] = "utf8mb4" if encoding.lower().replace("-", "") == "utf8" else encoding
    query.setdefault("charset", "utf8mb4")
    auth = ""
    if username:
        auth = quote_plus(username)
        if password:
            auth += ":" + quote_plus(password)
        auth += "@"
    return "mysql+asyncmy://%s%s%s?%s" % (auth, parsed.netloc, parsed.path, urlencode(query))


def configured_database_url(config: Mapping[str, Any]) -> Optional[str]:
    return jdbc_to_async_url(config)
