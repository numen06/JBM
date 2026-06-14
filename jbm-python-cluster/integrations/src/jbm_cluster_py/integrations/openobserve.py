import base64
from typing import Any, Dict, List, Mapping, Optional

import httpx


class OpenObserveClient:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.base_url = str(config.get("url") or "").rstrip("/")
        self.org = str(config.get("org") or "default")
        self.timeout = float(config.get("timeout-seconds") or 30)
        self.auth_token = str(config.get("auth-token") or "")
        self.username = str(config.get("username") or "")
        self.password = str(config.get("password") or "")
        self._client: Optional[httpx.AsyncClient] = None

    async def start(self) -> None:
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=self.timeout, headers=self._headers())

    async def stop(self) -> None:
        if self._client is not None:
            await self._client.aclose()
            self._client = None

    def _headers(self) -> Dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if self.auth_token:
            headers["Authorization"] = self.auth_token
        elif self.username and self.password:
            encoded = base64.b64encode(("%s:%s" % (self.username, self.password)).encode("utf-8")).decode("ascii")
            headers["Authorization"] = "Basic %s" % encoded
        return headers

    def _require_client(self) -> httpx.AsyncClient:
        if not self.base_url:
            raise RuntimeError("OpenObserve url is not configured")
        if self._client is None:
            raise RuntimeError("OpenObserve client is not started")
        return self._client

    async def ingest_json(self, stream: str, records: List[Mapping[str, Any]]) -> Dict[str, Any]:
        response = await self._require_client().post(
            "%s/api/%s/%s/_json" % (self.base_url, self.org, stream),
            json=list(records),
        )
        response.raise_for_status()
        return response.json() if response.content else {}

    async def search(
        self,
        sql: str,
        start_time: Optional[int] = None,
        end_time: Optional[int] = None,
        offset: int = 0,
        size: int = 100,
    ) -> Dict[str, Any]:
        query: Dict[str, Any] = {"sql": sql, "from": offset, "size": size}
        if start_time is not None:
            query["start_time"] = start_time
        if end_time is not None:
            query["end_time"] = end_time
        response = await self._require_client().post(
            "%s/api/%s/_search" % (self.base_url, self.org),
            json={"query": query, "search_type": "ui", "timeout": 0},
        )
        response.raise_for_status()
        return response.json()
