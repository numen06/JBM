from __future__ import annotations

import hashlib
import json
from datetime import datetime
from typing import Any, Mapping

import httpx

from jbm_cluster_py.integrations.nacos import NacosDiscoveryClient
from jbm_cluster_py.platform.center.modules.governance.infrastructure.crud_store import CrudStore


class OpenApiCatalog:
    """Discovers trusted cluster services and persists their OpenAPI contracts."""

    def __init__(self, store: CrudStore, discovery: NacosDiscoveryClient, client: httpx.AsyncClient) -> None:
        self.store = store
        self.discovery = discovery
        self.client = client

    async def sources(self) -> list[dict[str, Any]]:
        documents, _ = await self.store.list("openApiDocument", {}, 1, 100)
        routes, _ = await self.store.list("route", {}, 1, 100)
        by_service = {str(row.get("serviceId")): row for row in documents if row.get("serviceId")}
        for route in routes:
            service_id = str(route.get("serviceId") or "").strip()
            if service_id and service_id not in by_service:
                by_service[service_id] = {"serviceId": service_id, "syncStatus": "PENDING"}
        return list(by_service.values())

    async def sync(self, requested: Any = None) -> dict[str, Any]:
        service_ids = _values(requested)
        if not service_ids:
            service_ids = [str(row["serviceId"]) for row in await self.sources() if row.get("serviceId")]
        results = []
        for service_id in dict.fromkeys(service_ids):
            try:
                results.append(await self._sync_one(service_id))
            except Exception as exc:
                results.append({"serviceId": service_id, "success": False, "message": str(exc)})
        return {
            "synced": sum(1 for item in results if item.get("success")),
            "failed": sum(1 for item in results if not item.get("success")),
            "sources": results,
        }

    async def execute(self, request: Mapping[str, Any]) -> dict[str, Any]:
        service_id = str(request.get("serviceId") or "").strip()
        path = str(request.get("path") or "").strip()
        method = str(request.get("method") or request.get("requestMethod") or "GET").upper()
        if not service_id or not path.startswith("/") or "://" in path or ".." in path:
            raise ValueError("必须提供合法的 serviceId 和相对 path")
        if method not in {"GET", "POST", "PUT", "PATCH", "DELETE"}:
            raise ValueError("不支持的请求方法")
        base_url = await self._base_url(service_id)
        headers = {
            str(key): str(value)
            for key, value in dict(request.get("headers") or {}).items()
            if str(key).lower() not in {"host", "content-length", "connection"}
        }
        response = await self.client.request(
            method,
            base_url + path,
            params=request.get("query") or None,
            json=request.get("body") if method != "GET" else None,
            headers=headers,
            timeout=15,
        )
        content = response.content[:1_048_576]
        try:
            body: Any = json.loads(content)
        except (json.JSONDecodeError, UnicodeDecodeError):
            body = content.decode(errors="replace")
        return {"executed": True, "status": response.status_code, "headers": dict(response.headers), "body": body}

    async def spec(self, service_id: str) -> dict[str, Any]:
        rows, _ = await self.store.list("openApiDocument", {"serviceId": service_id}, 1, 1)
        if not rows:
            await self._sync_one(service_id)
            rows, _ = await self.store.list("openApiDocument", {"serviceId": service_id}, 1, 1)
        return _json(rows[0].get("rawSpec"), {}) if rows else {}

    async def _sync_one(self, service_id: str) -> dict[str, Any]:
        base_url = await self._base_url(service_id)
        spec = None
        source_url = ""
        for candidate in ("/v3/api-docs", "/v2/api-docs", "/openapi.json"):
            response = await self.client.get(base_url + candidate, timeout=15)
            if response.status_code < 400:
                spec = response.json()
                source_url = base_url + candidate
                break
        if not isinstance(spec, Mapping):
            raise ValueError(f"无法获取 {service_id} 的 OpenAPI 文档")
        raw = json.dumps(spec, ensure_ascii=False, separators=(",", ":"))
        digest = hashlib.sha256(raw.encode()).hexdigest()
        existing, _ = await self.store.list("openApiDocument", {"serviceId": service_id}, 1, 1)
        info = dict(spec.get("info") or {})
        document = await self.store.save(
            "openApiDocument",
            {
                "serviceId": service_id,
                "title": info.get("title"),
                "version": info.get("version"),
                "sourceUrl": source_url,
                "specVersion": spec.get("openapi") or spec.get("swagger"),
                "rawSpec": raw,
                "sourceHash": digest,
                "syncStatus": "SUCCESS",
                "syncMessage": None,
                "syncTime": datetime.now(),
            },
            existing[0].get("docId") if existing else None,
        )
        count = 0
        for path, path_item in dict(spec.get("paths") or {}).items():
            if not isinstance(path_item, Mapping):
                continue
            for method, operation in path_item.items():
                if method.upper() not in {"GET", "POST", "PUT", "PATCH", "DELETE"} or not isinstance(operation, Mapping):
                    continue
                key = f"{service_id}:{method.upper()}:{path}"
                old, _ = await self.store.list("openApiOperation", {"operationKey": key}, 1, 1)
                await self.store.save(
                    "openApiOperation",
                    {
                        "docId": document.get("docId"),
                        "serviceId": service_id,
                        "path": path,
                        "requestMethod": method.upper(),
                        "tags": json.dumps(operation.get("tags") or [], ensure_ascii=False),
                        "summary": operation.get("summary"),
                        "description": operation.get("description"),
                        "operationKey": key,
                        "parametersJson": json.dumps(operation.get("parameters") or [], ensure_ascii=False),
                        "requestBodyJson": json.dumps(operation.get("requestBody") or {}, ensure_ascii=False),
                        "responsesJson": json.dumps(operation.get("responses") or {}, ensure_ascii=False),
                        "securityJson": json.dumps(operation.get("security") or [], ensure_ascii=False),
                        "rawOperationJson": json.dumps(operation, ensure_ascii=False),
                        "deprecated": int(bool(operation.get("deprecated"))),
                        "syncState": "ACTIVE",
                        "sourceHash": hashlib.sha256(json.dumps(operation, sort_keys=True).encode()).hexdigest(),
                        "syncTime": datetime.now(),
                    },
                    old[0].get("operationId") if old else None,
                )
                count += 1
        return {"serviceId": service_id, "success": True, "operations": count, "sourceUrl": source_url}

    async def _base_url(self, service_id: str) -> str:
        instance = await self.discovery.choose_instance(service_id)
        if instance:
            scheme = "https" if str(instance.get("metadata", {}).get("secure", "false")).lower() == "true" else "http"
            return f"{scheme}://{instance['ip']}:{instance['port']}"
        routes, _ = await self.store.list("route", {"serviceId": service_id}, 1, 10)
        for route in routes:
            url = str(route.get("url") or "").rstrip("/")
            if url.startswith(("http://", "https://")):
                return url
        raise ValueError(f"服务不在可信集群发现列表中: {service_id}")


def _values(value: Any) -> list[str]:
    if isinstance(value, str):
        return [item.strip() for item in value.split(",") if item.strip()]
    if isinstance(value, (list, tuple, set)):
        return [str(item).strip() for item in value if str(item).strip()]
    return []


def _json(value: Any, default: Any) -> Any:
    if not isinstance(value, str):
        return value if value is not None else default
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return default
