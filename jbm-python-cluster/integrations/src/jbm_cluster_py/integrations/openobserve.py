import base64
import logging
import re
from typing import Any, Dict, List, Mapping, Optional

import httpx

logger = logging.getLogger(__name__)

COUNT_FIELD = "zo_sql_num"


class OpenObserveClient:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.base_url = str(config.get("url") or "").rstrip("/")
        self.org = str(config.get("org") or config.get("organization") or "default")
        self.timeout = float(config.get("timeout-seconds") or 30)
        self.auth_token = str(config.get("auth-token") or "")
        self.username = str(config.get("username") or "")
        self.password = str(config.get("password") or "")
        self._client: Optional[httpx.AsyncClient] = None

    @property
    def enabled(self) -> bool:
        return bool(self.base_url) and bool(self.auth_token or (self.username and self.password))

    async def start(self) -> None:
        if not self.enabled:
            logger.info("OpenObserve client disabled: missing url or credentials")
            return
        if self._client is None:
            self._client = httpx.AsyncClient(
                timeout=httpx.Timeout(connect=10.0, read=60.0, write=30.0, pool=30.0),
                headers=self._headers(),
            )

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
        if not self.enabled:
            raise RuntimeError("OpenObserve is not configured")
        if self._client is None:
            raise RuntimeError("OpenObserve client is not started")
        return self._client

    async def ingest_json(self, stream: str, records: List[Mapping[str, Any]]) -> Dict[str, Any]:
        if not records:
            return {}
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
        order_by: str = "desc",
    ) -> Dict[str, Any]:
        query: Dict[str, Any] = {
            "sql": sql,
            "from": max(offset, 0),
            "size": size,
            "orderBy": order_by,
        }
        if start_time is not None:
            query["start_time"] = start_time
        if end_time is not None:
            query["end_time"] = end_time
        response = await self._require_client().post(
            "%s/api/%s/_search" % (self.base_url, self.org),
            json={"timeout": 0, "query": query},
        )
        response.raise_for_status()
        result = response.json()
        if result.get("code") not in (None, 200) and not result.get("hits"):
            message = result.get("message") or result.get("error")
            if message:
                logger.warning("OpenObserve search HTTP error: %s", message)
        return result

    async def count(
        self,
        sql: str,
        start_time: Optional[int] = None,
        end_time: Optional[int] = None,
    ) -> int:
        count_sql = re.sub(r"\s+ORDER BY\s+.+$", "", sql, flags=re.IGNORECASE)
        count_sql = re.sub(
            r"(?is)SELECT\s+.*?\s+FROM",
            "SELECT COUNT(*) AS %s FROM" % COUNT_FIELD,
            count_sql,
            count=1,
        )
        query: Dict[str, Any] = {
            "sql": count_sql,
            "from": 0,
            "size": -1,
            "orderBy": "desc",
        }
        if start_time is not None:
            query["start_time"] = start_time
        if end_time is not None:
            query["end_time"] = end_time
        response = await self._require_client().post(
            "%s/api/%s/_search" % (self.base_url, self.org),
            json={"timeout": 0, "query": query},
        )
        response.raise_for_status()
        result = response.json()
        if result.get("code") not in (None, 200) and not result.get("hits"):
            message = result.get("message") or result.get("error")
            if message:
                logger.warning("OpenObserve count HTTP error: %s", message)
        hits = list(result.get("hits") or [])
        if hits:
            value = hits[0].get(COUNT_FIELD)
            if value is not None:
                return int(value)
        return int(result.get("scan_records") or result.get("total") or 0)
