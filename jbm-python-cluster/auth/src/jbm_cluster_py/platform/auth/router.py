from __future__ import annotations

import base64
from typing import Any, Mapping

from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.result import fail, ok
from jbm_cluster_py.platform.auth.jwt import JwtError
from jbm_cluster_py.platform.auth.service import AuthError, AuthService


def build_auth_router(auth_service: AuthService) -> APIRouter:
    router = APIRouter()

    @router.post("/oauth2/token")
    async def token(request: Request) -> JSONResponse:
        form = await _request_params(request)
        form["password_encrypted"] = _password_encrypted(request)
        try:
            grant_type = str(form.get("grant_type") or "").strip()
            if grant_type == "password":
                result = await auth_service.password_token(form)
            elif grant_type == "client_credentials":
                result = await auth_service.client_credentials_token(form)
            elif grant_type == "refresh_token":
                result = await auth_service.refresh_token(form)
            else:
                raise AuthError("不支持的grant_type: %s" % (grant_type or "<empty>"), 400)
            return JSONResponse(status_code=200, content=ok(result))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.post("/oauth2/refresh")
    async def refresh(request: Request) -> JSONResponse:
        form = await _request_params(request)
        form.setdefault("grant_type", "refresh_token")
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.refresh_token(form)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.post("/oauth2/client_token")
    async def client_token(request: Request) -> JSONResponse:
        form = await _request_params(request)
        form.setdefault("grant_type", "client_credentials")
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.client_credentials_token(form)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.get("/oauth2/userinfo")
    async def userinfo_get(request: Request) -> JSONResponse:
        return await _userinfo(request, auth_service)

    @router.post("/oauth2/userinfo")
    async def userinfo_post(request: Request) -> JSONResponse:
        return await _userinfo(request, auth_service)

    @router.delete("/oauth2/logout")
    @router.post("/oauth2/logout")
    async def logout(request: Request) -> JSONResponse:
        params = await _request_params(request)
        token = _bearer_token(request) or str(params.get("access_token") or "")
        refresh_token = str(params.get("refresh_token") or "")
        return JSONResponse(status_code=200, content=ok(await auth_service.logout(token, refresh_token)))

    @router.get("/oauth2/publicKey")
    async def public_key(app_id: str = "") -> JSONResponse:
        public_key_value = await auth_service.public_key(app_id)
        if not public_key_value:
            return JSONResponse(status_code=200, content=fail(None, "客户端不存在或未配置公钥", 404))
        return JSONResponse(status_code=200, content=ok(public_key_value))

    @router.get("/.well-known/openid-configuration")
    async def openid_configuration() -> dict[str, Any]:
        return auth_service.openid_configuration()

    @router.get("/jwks.json")
    async def jwks() -> dict[str, Any]:
        return auth_service.jwks()

    @router.post("/online/pageList")
    async def online_users(request: Request) -> JSONResponse:
        params = await _request_params(request)
        try:
            _require_login(request, auth_service)
            return JSONResponse(status_code=200, content=ok(await auth_service.online_users(params)))
        except (AuthError, JwtError) as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))

    @router.delete("/online/kickout/{token_id:path}")
    async def online_kickout(token_id: str, request: Request) -> JSONResponse:
        return await _online_revoke(
            token_id,
            request,
            auth_service,
            "ACTION_monitor:online:forceLogout",
            "monitor:online:forceLogout",
        )

    @router.delete("/online/logout/{token_id:path}")
    async def online_logout(token_id: str, request: Request) -> JSONResponse:
        return await _online_revoke(
            token_id,
            request,
            auth_service,
            "ACTION_monitor:online:logout",
            "monitor:online:logout",
        )

    @router.post("/online/expire")
    async def online_expire(request: Request) -> JSONResponse:
        params = await _request_params(request)
        token_id = str(params.get("tokenId") or params.get("token_id") or "")
        try:
            _require_login(request, auth_service)
            if not token_id:
                raise AuthError("tokenId不能为空", 400)
            return JSONResponse(status_code=200, content=ok("设置成功"))
        except (AuthError, JwtError) as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))

    @router.post("/online/expireImmediately")
    async def online_expire_immediately(request: Request) -> JSONResponse:
        params = await _request_params(request)
        token_id = str(params.get("tokenId") or params.get("token_id") or "")
        try:
            _require_login(request, auth_service)
            if not token_id:
                raise AuthError("tokenId不能为空", 400)
            await auth_service.revoke_access_token(token_id)
            return JSONResponse(status_code=200, content=ok("设置成功"))
        except (AuthError, JwtError) as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))

    return router


async def _userinfo(request: Request, auth_service: AuthService) -> JSONResponse:
    token = _bearer_token(request) or str(request.query_params.get("access_token") or "")
    if not token:
        return JSONResponse(status_code=200, content=fail(None, "未提供access_token", 401))
    try:
        return JSONResponse(status_code=200, content=ok(await auth_service.userinfo(token)))
    except (AuthError, JwtError) as exc:
        return JSONResponse(status_code=200, content=fail(None, "无效的access_token: %s" % exc, 401))


async def _request_params(request: Request) -> dict[str, Any]:
    params: dict[str, Any] = {}
    auth_header = request.headers.get("authorization") or ""
    if auth_header.lower().startswith("basic "):
        try:
            client_id, client_secret = (
                base64.b64decode(auth_header.split(None, 1)[1]).decode("utf-8").split(":", 1)
            )
            params["client_id"] = client_id
            params["client_secret"] = client_secret
        except Exception:
            pass
    content_type = request.headers.get("content-type", "")
    if "application/json" in content_type:
        body = await request.json()
        if isinstance(body, Mapping):
            params.update(dict(body))
    elif request.method in {"POST", "PUT", "PATCH", "DELETE"}:
        form = await request.form()
        params.update(dict(form))
    params.update(dict(request.query_params))
    return params


def _bearer_token(request: Request) -> str:
    auth_header = request.headers.get("authorization") or ""
    if auth_header.lower().startswith("bearer "):
        return auth_header.split(None, 1)[1].strip()
    return ""


def _require_login(request: Request, auth_service: AuthService) -> str:
    token = _bearer_token(request)
    auth_service.verify_permissions(token)
    return token


async def _online_revoke(
    token_id: str,
    request: Request,
    auth_service: AuthService,
    *permissions: str,
) -> JSONResponse:
    try:
        auth_service.verify_permissions(_bearer_token(request), *permissions)
        if not token_id:
            raise AuthError("tokenId不能为空", 400)
        await auth_service.revoke_access_token(token_id)
        return JSONResponse(status_code=200, content=ok(None))
    except (AuthError, JwtError) as exc:
        return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))


def _password_encrypted(request: Request) -> bool:
    value = request.headers.get("X-Password-Encrypted") or request.headers.get("x-password-encrypted") or ""
    return value.strip().lower() in {"1", "true", "yes", "y"}
