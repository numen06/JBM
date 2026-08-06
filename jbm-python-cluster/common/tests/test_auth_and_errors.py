import httpx
import pytest
from fastapi import FastAPI
from jbm_cluster_py.common.auth import UserInfoAuthClient
from jbm_cluster_py.common.errors import install_exception_handlers


@pytest.mark.asyncio
async def test_userinfo_auth_rejects_missing_token_and_accepts_verified_identity() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.headers["Authorization"] != "Bearer valid":
            return httpx.Response(
                401, json={"success": False, "message": "访问令牌无效"}
            )
        return httpx.Response(
            200,
            json={"success": True, "result": {"userId": 42, "tenantId": 7}},
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        auth = UserInfoAuthClient({"enabled": True}, client)
        with pytest.raises(PermissionError):
            await auth.authenticate("Bearer forged")
        assert await auth.authenticate("Bearer valid") == {"userId": 42, "tenantId": 7}


@pytest.mark.asyncio
async def test_userinfo_auth_reports_upstream_failure_as_unavailable() -> None:
    async with httpx.AsyncClient(
        transport=httpx.MockTransport(
            lambda request: httpx.Response(503, json={"success": False})
        )
    ) as client:
        auth = UserInfoAuthClient({"enabled": True}, client)
        with pytest.raises(ConnectionError):
            await auth.authenticate("Bearer valid")


@pytest.mark.asyncio
async def test_error_handlers_use_real_status_and_hide_internal_exception() -> None:
    app = FastAPI()
    install_exception_handlers(app)

    @app.get("/bad")
    async def bad() -> None:
        raise ValueError("bad input")

    @app.get("/v1/bad")
    async def v1_bad() -> None:
        raise ValueError("bad input")

    @app.get("/boom")
    async def boom() -> None:
        raise RuntimeError("mysql://user:secret@db")

    transport = httpx.ASGITransport(app=app, raise_app_exceptions=False)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        bad_response = await client.get("/bad")
        boom_response = await client.get("/boom")
        traced_response = await client.get(
            "/bad", headers={"X-Request-ID": "client-request-123"}
        )
        problem_response = await client.get(
            "/v1/bad", headers={"X-Request-ID": "problem-request-123"}
        )

    assert bad_response.status_code == 400
    assert bad_response.json()["code"] == 400
    assert boom_response.status_code == 500
    assert boom_response.json()["message"] == "服务异常"
    assert "secret" not in boom_response.text
    assert traced_response.headers["X-Request-ID"] == "client-request-123"
    assert traced_response.headers["Server-Timing"].startswith("app;dur=")
    assert problem_response.status_code == 400
    assert problem_response.headers["content-type"].startswith("application/problem+json")
    assert problem_response.json()["requestId"] == "problem-request-123"
