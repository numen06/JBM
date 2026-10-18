"""Framework decision point using current identities and existing API/authority tables."""

from __future__ import annotations

import base64
import hashlib
import re
import time
from datetime import datetime
from typing import Any

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from fastapi import HTTPException
from sqlalchemy import text

from jbm_cluster_py.common.api_access import (
    authorize_api,
    is_platform,
    metadata_path,
    normalized_path,
    permission_codes,
    require_api_boundary,
    signature_payload,
    tenant_administration_api,
)


class RequestAccessService:
    def __init__(self, repository: Any, redis: Any, auth: Any):
        self.repository, self.redis, self.auth = repository, redis, auth

    async def api(self, service: str, method: str, path: str) -> dict[str, Any] | None:
        async with self.repository.engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        text("SELECT * FROM base_api WHERE service_id=:service"),
                        {"service": service},
                    )
                )
                .mappings()
                .all()
            )
        matched = []
        for row in rows:
            pattern = str(row.get("path") or "")
            if not pattern.startswith("/"):
                continue
            expression = re.sub(r"\\\{[^}]+\\\}", r"[^/]+", re.escape(pattern.rstrip("/")))
            methods = {x.strip().upper() for x in str(row.get("request_method") or "").split(",")}
            if method.upper() in methods and re.fullmatch(expression, normalized_path(path)):
                matched.append(dict(row))
        return (
            max(matched, key=lambda row: len(re.sub(r"\{[^}]+\}", "", row["path"])))
            if matched
            else None
        )

    async def check(self, body: dict[str, Any], authorization: str) -> dict[str, Any]:
        service = str(body.get("service") or "")
        method = str(body.get("method") or "").upper()
        path = normalized_path(str(body.get("path") or ""))
        if not re.fullmatch(r"[a-zA-Z0-9_-]{1,100}", service) or method not in {
            "GET",
            "HEAD",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS",
        }:
            raise HTTPException(403, "非法接口标识")
        if metadata_path(path):
            if body.get("key"):
                raise HTTPException(403, "API Key不能访问内部接口文档")
            identity = await self.auth.authenticate(authorization)
            authorize_api(service, method, path, identity)
            return {"allowed": True, "identity": identity}
        api = await self.api(service, method, path)
        if body.get("key"):
            return await self.signed(body, api)
        identity = await self.auth.authenticate(authorization)
        require_api_boundary(service, path, identity)
        if api is not None:
            if api.get("status") != 1:
                raise HTTPException(403, "接口已停用")
            async with self.repository.engine.connect() as conn:
                authorities = (
                    (
                        await conn.execute(
                            text(
                                "SELECT authority FROM base_authority WHERE api_id=:api AND status=1 AND (app_id IS NULL OR app_id=:app)"
                            ),
                            {"api": api["api_id"], "app": identity.get("appId")},
                        )
                    )
                    .scalars()
                    .all()
                )
                action_codes = (
                    (
                        await conn.execute(
                            text(
                                "SELECT DISTINCT aa.authority FROM base_authority_action link JOIN base_authority ap ON ap.authority_id=link.authority_id JOIN base_authority aa ON aa.action_id=link.action_id WHERE ap.api_id=:api AND ap.status=1 AND aa.status=1 AND (aa.app_id IS NULL OR aa.app_id=:app) AND (link.expire_time IS NULL OR link.expire_time>CURRENT_TIMESTAMP)"
                            ),
                            {"api": api["api_id"], "app": identity.get("appId")},
                        )
                    )
                    .scalars()
                    .all()
                )
            if not is_platform(identity) and not tenant_administration_api(service, method, path, identity) and not (
                set(authorities) | set(action_codes)
            ) & permission_codes(identity):
                raise HTTPException(403, "未获准调用此API")
        else:
            authorize_api(service, method, path, identity)
        return {"allowed": True, "identity": identity}

    async def signed(self, body: dict[str, Any], api: dict[str, Any] | None) -> dict[str, Any]:
        if api is None or api.get("status") != 1 or api.get("is_open") != 1:
            raise HTTPException(403, "接口未向API Key开放")
        nonce, stamp = str(body.get("nonce") or ""), str(body.get("timestamp") or "")
        if (
            not re.fullmatch(r"[A-Za-z0-9_-]{16,128}", nonce)
            or not re.fullmatch(r"[0-9]{13}", stamp)
            or abs(time.time() - int(stamp) / 1000) > 300
        ):
            raise HTTPException(401, "签名时间或nonce无效")
        key = str(body["key"])
        async with self.repository.engine.connect() as conn:
            row = (
                (
                    await conn.execute(
                        text(
                            "SELECT k.*, u.company_id AS owner_tenant, d.user_id AS owner_user, u.status AS owner_status FROM base_api_key k JOIN base_app a ON a.app_id=COALESCE(k.biz_app_id,k.app_id) JOIN base_developer d ON d.developer_id=k.developer_id JOIN base_user u ON u.user_id=d.user_id WHERE k.api_key=:key"
                        ),
                        {"key": key},
                    )
                )
                .mappings()
                .first()
            )
            if (
                not row
                or row.get("status") != 1
                or row.get("owner_status") != 1
                or row.get("revoke_time")
                or (row.get("expire_time") and row["expire_time"] <= datetime.now())
            ):
                raise HTTPException(401, "API Key无效或已停用")
            granted = (
                await conn.execute(
                    text(
                        "SELECT 1 FROM base_authority_apikey g JOIN base_authority a ON a.authority_id=g.authority_id WHERE g.key_id=:key AND a.api_id=:api AND a.status=1 AND (g.auth_status IS NULL OR g.auth_status=1) AND (g.expire_time IS NULL OR g.expire_time>CURRENT_TIMESTAMP) LIMIT 1"
                    ),
                    {"key": row["key_id"], "api": api["api_id"]},
                )
            ).first()
        if not granted:
            raise HTTPException(403, "API Key未获得此API授权")
        if not row.get("owner_tenant") or int(row["owner_tenant"]) <= 0:
            raise HTTPException(403, "API Key开发者未绑定数据租户")
        tenant = str(body.get("tenant") or "")
        if tenant and tenant != str(row["owner_tenant"]):
            raise HTTPException(403, "API Key不能切换数据租户")
        payload = signature_payload(
            str(body["service"]),
            str(body["method"]),
            str(body["path"]),
            str(body.get("query") or ""),
            str(body.get("bodyHash") or ""),
            key,
            stamp,
            nonce,
            tenant,
            str(body.get("project") or ""),
        )
        try:
            encoded = str(row.get("public_key") or "").encode()
            public = (
                serialization.load_pem_public_key(encoded)
                if b"BEGIN" in encoded
                else serialization.load_der_public_key(base64.b64decode(encoded, validate=True))
            )
            if not isinstance(public, rsa.RSAPublicKey) or public.key_size < 2048:
                raise ValueError("unsafe key")
            public.verify(
                base64.b64decode(str(body.get("signature") or ""), validate=True),
                payload,
                padding.PKCS1v15(),
                hashes.SHA256(),
            )
        except Exception:
            raise HTTPException(401, "请求签名无效") from None
        if self.redis.client is None:
            raise HTTPException(503, "防重放存储不可用")
        replay = hashlib.sha256((key + ":" + nonce).encode()).hexdigest()
        if not await self.redis.client.set("jbm:request-nonce:" + replay, "1", nx=True, ex=610):
            raise HTTPException(401, "重复的签名请求")
        identity = {
            "userId": row["owner_user"],
            "appId": row.get("biz_app_id") or row.get("app_id"),
            "tenantId": row["owner_tenant"],
            "clientId": key,
            "credentialType": "api_key",
            "permissions": [],
            "roles": [],
        }
        require_api_boundary(str(body.get("service") or ""), str(body.get("path") or ""), identity)
        return {
            "allowed": True,
            "identity": identity,
        }
