from __future__ import annotations

import asyncio
import logging
import time
from dataclasses import dataclass
from fnmatch import fnmatchcase
from typing import Any, Mapping, Optional
from urllib.parse import urlsplit, urlunsplit

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine

from jbm_cluster_py.integrations.database import configured_database_url

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class GatewayRoute:
    route_name: str
    path: str
    service_id: Optional[str] = None
    url: Optional[str] = None
    strip_prefix: int = 1

    @property
    def target_label(self) -> str:
        return self.url or self.service_id or self.route_name


def _camel_or_snake(row: Mapping[str, Any], key: str, default: Any = None) -> Any:
    if key in row:
        return row[key]
    snake = ""
    for char in key:
        if char.isupper():
            snake += "_" + char.lower()
        else:
            snake += char
    return row.get(snake, default)


def _route_from_mapping(row: Mapping[str, Any]) -> GatewayRoute:
    route_name = str(_camel_or_snake(row, "routeName") or _camel_or_snake(row, "routeId") or "route")
    strip_prefix = _camel_or_snake(row, "stripPrefix", 1)
    return GatewayRoute(
        route_name=route_name,
        path=str(_camel_or_snake(row, "path") or "/**"),
        service_id=_none_if_blank(_camel_or_snake(row, "serviceId")),
        url=_none_if_blank(_camel_or_snake(row, "url")),
        strip_prefix=int(strip_prefix if strip_prefix is not None else 1),
    )


def _none_if_blank(value: Any) -> Optional[str]:
    if value is None:
        return None
    text_value = str(value).strip()
    return text_value or None


def _matches(pattern: str, path: str) -> bool:
    if not pattern.startswith("/"):
        pattern = "/" + pattern
    if pattern.endswith("/**"):
        prefix = pattern[:-3]
        return path == prefix or path.startswith(prefix.rstrip("/") + "/")
    if pattern.endswith("**"):
        return path.startswith(pattern[:-2])
    return fnmatchcase(path, pattern.replace("**", "*"))


def strip_prefix(path: str, strip_count: int) -> str:
    if strip_count <= 0:
        return path or "/"
    parts = [part for part in path.split("/") if part]
    stripped = parts[strip_count:]
    return "/" + "/".join(stripped) if stripped else "/"


def join_target_url(base_url: str, path: str, query: str = "") -> str:
    parsed = urlsplit(base_url)
    base_path = parsed.path.rstrip("/")
    target_path = path if path.startswith("/") else "/" + path
    full_path = (base_path + target_path) or "/"
    return urlunsplit((parsed.scheme, parsed.netloc, full_path, query, ""))


class RouteRepository:
    def __init__(self, database_config: Mapping[str, Any], fallback_routes: list[Mapping[str, Any]]) -> None:
        self.database_config = dict(database_config)
        self.fallback_routes = [_route_from_mapping(row) for row in fallback_routes]
        self.routes: list[GatewayRoute] = []
        self.loaded_from = "empty"
        self.updated_at = 0.0
        self._engine: Optional[AsyncEngine] = None
        self._lock = asyncio.Lock()

    async def start(self) -> None:
        url = configured_database_url(self.database_config)
        if url:
            self._engine = create_async_engine(url, pool_pre_ping=True)
        await self.reload()

    async def stop(self) -> None:
        if self._engine is not None:
            await self._engine.dispose()
            self._engine = None

    async def reload(self) -> list[GatewayRoute]:
        async with self._lock:
            loaded = await self._load_database_routes()
            if loaded:
                self.routes = loaded
                self.loaded_from = "database"
            else:
                self.routes = list(self.fallback_routes)
                self.loaded_from = "fallback"
            self._ensure_compat_routes()
            self.routes.sort(key=lambda item: len(item.path), reverse=True)
            self.updated_at = time.time()
            logger.info("Loaded %s gateway routes from %s", len(self.routes), self.loaded_from)
            return list(self.routes)

    async def _load_database_routes(self) -> list[GatewayRoute]:
        if self._engine is None:
            return []
        sql = text(
            """
            select route_id, route_name, path, service_id, url, strip_prefix
            from gateway_route
            where status = 1
            order by create_time desc
            """
        )
        try:
            async with self._engine.connect() as conn:
                result = await conn.execute(sql)
                rows = [dict(row._mapping) for row in result.fetchall()]
        except Exception as exc:
            logger.warning("Failed to load gateway_route; fallback routes will be used: %s", exc)
            return []
        return [_route_from_mapping(row) for row in rows if row.get("path")]

    def match(self, path: str) -> Optional[GatewayRoute]:
        for route in self.routes:
            if _matches(route.path, path):
                return route
        return None

    def _ensure_compat_routes(self) -> None:
        service_prefixes = (
            ("auth", "jbm-cluster-platform-auth"),
            ("center", "jbm-cluster-platform-center"),
            ("doc", "jbm-cluster-platform-doc"),
            ("push", "jbm-cluster-platform-push"),
            ("logs", "jbm-cluster-platform-logs"),
            ("job", "jbm-cluster-platform-job"),
            ("bigscreen", "jbm-cluster-platform-bigscreen"),
        )
        for prefix, service_id in service_prefixes:
            path = f"/{prefix}/**"
            expected = GatewayRoute(
                route_name=f"{prefix}-service",
                path=path,
                service_id=service_id,
                strip_prefix=1,
            )
            existing_index = next(
                (index for index, route in enumerate(self.routes) if route.path == path),
                None,
            )
            if existing_index is None:
                self.routes.append(expected)
            else:
                existing = self.routes[existing_index]
                if existing.service_id == service_id and existing.strip_prefix != 1:
                    self.routes[existing_index] = GatewayRoute(
                        route_name=existing.route_name,
                        path=existing.path,
                        service_id=existing.service_id,
                        url=existing.url,
                        strip_prefix=1,
                    )
        if not any(route.path == "/online/**" for route in self.routes):
            self.routes.append(
                GatewayRoute(
                    route_name="auth-online",
                    path="/online/**",
                    service_id="jbm-cluster-platform-auth",
                    strip_prefix=0,
                )
            )

    def snapshot(self) -> list[dict[str, Any]]:
        return [
            {
                "routeName": route.route_name,
                "path": route.path,
                "serviceId": route.service_id,
                "url": route.url,
                "stripPrefix": route.strip_prefix,
            }
            for route in self.routes
        ]
