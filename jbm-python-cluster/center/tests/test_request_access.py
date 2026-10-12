import asyncio
import base64
import hashlib
import time
from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
import httpx
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from fastapi import HTTPException
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

from jbm_cluster_py.common.api_access import signature_payload
from jbm_cluster_py.common.auth import UserInfoAuthClient
from jbm_cluster_py.platform.center.modules.governance.application.request_access import (
    RequestAccessService,
)


class Nonces:
    def __init__(self):
        self.used = set()

    async def set(self, key, value, nx, ex):
        if key in self.used:
            return False
        self.used.add(key)
        return True


@pytest.fixture
async def signed_service():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    private = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    public = (
        private.public_key()
        .public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo)
        .decode()
    )
    statements = [
        "CREATE TABLE base_api(api_id INTEGER,service_id TEXT,path TEXT,request_method TEXT,status INTEGER,is_open INTEGER)",
        "CREATE TABLE base_app(app_id INTEGER,org_id INTEGER)",
        "CREATE TABLE base_developer(developer_id INTEGER,user_id INTEGER)",
        "CREATE TABLE base_user(user_id INTEGER,status INTEGER,company_id INTEGER)",
        "CREATE TABLE base_api_key(key_id INTEGER,api_key TEXT,app_id INTEGER,biz_app_id INTEGER,developer_id INTEGER,status INTEGER,revoke_time DATETIME,expire_time DATETIME,public_key TEXT)",
        "CREATE TABLE base_authority(authority_id TEXT,api_id INTEGER,status INTEGER,authority TEXT,app_id INTEGER,action_id INTEGER)",
        "CREATE TABLE base_authority_apikey(key_id INTEGER,authority_id TEXT,auth_status INTEGER,expire_time DATETIME)",
        "CREATE TABLE base_authority_action(authority_id TEXT,action_id INTEGER,expire_time DATETIME)",
        "INSERT INTO base_api VALUES(1,'iot-platform','/devices/{id}','GET',1,1)",
        "INSERT INTO base_api VALUES(2,'iot-platform','/devices/{id}','DELETE',1,1)",
        "INSERT INTO base_app VALUES(10,99)",
        "INSERT INTO base_developer VALUES(20,42)",
        "INSERT INTO base_user VALUES(42,1,7)",
        "INSERT INTO base_authority VALUES('a1',1,1,'API_read',10,NULL)",
        "INSERT INTO base_authority_apikey VALUES(30,'a1',1,NULL)",
    ]
    async with engine.begin() as conn:
        for sql in statements:
            await conn.execute(text(sql))
        await conn.execute(
            text("INSERT INTO base_api_key VALUES(30,'test-key',10,10,20,1,NULL,NULL,:public)"),
            {"public": public},
        )
    auth = SimpleNamespace(
        authenticate=AsyncMock(
            return_value={"userId": 42, "appId": 10, "tenantId": 7, "permissions": ["API_read"]}
        )
    )
    svc = RequestAccessService(
        SimpleNamespace(engine=engine), SimpleNamespace(client=Nonces()), auth
    )
    yield svc, private
    await engine.dispose()


def proof(private, **changes):
    body = dict(
        service="iot-platform",
        method="GET",
        path="/devices/123",
        query="",
        bodyHash=hashlib.sha256(b"").hexdigest(),
        key="test-key",
        timestamp=str(int(time.time() * 1000)),
        nonce="0123456789abcdef",
        tenant="7",
        project="one",
    )
    body.update(changes)
    raw = signature_payload(
        body["service"],
        body["method"],
        body["path"],
        body["query"],
        body["bodyHash"],
        body["key"],
        body["timestamp"],
        body["nonce"],
        body["tenant"],
        body["project"],
    )
    body["signature"] = base64.b64encode(
        private.sign(raw, padding.PKCS1v15(), hashes.SHA256())
    ).decode()
    return body


@pytest.mark.asyncio
async def test_valid_signature_has_exact_api_grant_and_replay_is_atomic(signed_service):
    svc, private = signed_service
    body = proof(private)
    results = await asyncio.gather(svc.check(body, ""), svc.check(body, ""), return_exceptions=True)
    assert sum(isinstance(x, dict) and x["allowed"] for x in results) == 1
    assert sum(isinstance(x, HTTPException) and x.status_code == 401 for x in results) == 1
    with pytest.raises(HTTPException) as exc:
        await svc.check(proof(private, method="DELETE", nonce="differentnonce00"), "")
    assert exc.value.status_code == 403


@pytest.mark.asyncio
async def test_document_boundary_precedes_registered_api_grants(signed_service):
    svc, private = signed_service
    async with svc.repository.engine.begin() as conn:
        await conn.execute(text("INSERT INTO base_api VALUES(3,'push','/openapi.json','GET',1,1)"))
        await conn.execute(text("INSERT INTO base_authority VALUES('doc',3,1,'API_read',10,NULL)"))
        await conn.execute(text("INSERT INTO base_authority_apikey VALUES(30,'doc',1,NULL)"))
    body = {"service": "push", "method": "GET", "path": "/openapi.json"}
    with pytest.raises(HTTPException) as exc:
        await svc.check(body, "Bearer user")
    assert exc.value.status_code == 403
    with pytest.raises(HTTPException) as exc:
        await svc.check(proof(private, service="push", path="/openapi.json"), "")
    assert exc.value.status_code == 403
    svc.auth.authenticate.return_value = {"clientId": "gateway", "scope": "internal"}
    assert (await svc.check(body, "Bearer service"))["allowed"]


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "changed",
    [{"bodyHash": "0" * 64}, {"tenant": "8"}, {"project": "other"}, {"timestamp": "1000000000000"}],
)
async def test_tampering_and_expired_proof_are_denied(signed_service, changed):
    svc, private = signed_service
    body = proof(private)
    body.update(changed)
    with pytest.raises(HTTPException):
        await svc.check(body, "")


@pytest.mark.asyncio
async def test_revocation_is_checked_for_existing_token_and_key(signed_service):
    svc, private = signed_service
    bearer = dict(service="iot-platform", method="GET", path="/devices/123")
    assert (await svc.check(bearer, "Bearer unchanged"))["allowed"]
    svc.auth.authenticate.return_value["permissions"] = []
    with pytest.raises(HTTPException) as exc:
        await svc.check(bearer, "Bearer unchanged")
    assert exc.value.status_code == 403
    async with svc.repository.engine.begin() as conn:
        await conn.execute(text("UPDATE base_api_key SET status=0"))
    with pytest.raises(HTTPException) as exc:
        await svc.check(proof(private), "")
    assert exc.value.status_code == 401


@pytest.mark.asyncio
async def test_http_headers_use_signature_guard_before_business_handler(signed_service):
    service, private = signed_service
    writes = []
    app = FastAPI()
    async with httpx.AsyncClient() as upstream:
        auth = UserInfoAuthClient({}, upstream)
        auth.access_checker = service.check

        @app.middleware("http")
        async def guard(request: Request, call_next):
            try:
                request.state.identity = await auth.authenticate_request(request, "iot-platform")
            except HTTPException as exc:
                return JSONResponse(status_code=exc.status_code, content={"message": exc.detail})
            return await call_next(request)

        @app.get("/devices/{device_id}")
        async def endpoint(device_id: int):
            writes.append(device_id)
            return {"device": device_id}

        body = proof(private)
        headers = {
            "X-App-Id": body["key"],
            "X-Timestamp": body["timestamp"],
            "X-Nonce": body["nonce"],
            "X-Signature": body["signature"],
            "X-Signature-Version": "2",
            "X-Tenant-Id": "7",
            "X-Project-Id": "one",
        }
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app), base_url="http://terminal"
        ) as client:
            assert (await client.get("/devices/123", headers=headers)).status_code == 200
            assert (await client.get("/devices/123", headers=headers)).status_code == 401
            assert writes == [123]
            assert (
                await client.get("/devices/123", headers={"Authorization": "Bearer existing"})
            ).status_code == 200
