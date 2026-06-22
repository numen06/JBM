from __future__ import annotations

import base64
import html
from typing import Any, Mapping

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, JSONResponse

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
            elif grant_type == "authorization_code":
                result = await auth_service.authorization_code_token(form)
            else:
                raise AuthError("不支持的grant_type: %s" % (grant_type or "<empty>"), 400)
            return JSONResponse(status_code=200, content=ok(result))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.get("/oauth2/authorize")
    async def authorize(request: Request) -> HTMLResponse:
        params = dict(request.query_params)
        return HTMLResponse(_authorize_login_html(params))

    @router.post("/oauth2/doLogin")
    async def oauth_do_login(request: Request) -> JSONResponse:
        form = await _request_params(request)
        form["password_encrypted"] = _password_encrypted(request)
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.authorize_code_login(form)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.get("/oauth2/apps")
    async def oauth_apps() -> JSONResponse:
        return JSONResponse(status_code=200, content=ok(await auth_service.public_clients()))

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

    @router.post("/oauth2/register")
    async def register(request: Request) -> JSONResponse:
        form = await _request_params(request)
        form["password_encrypted"] = _password_encrypted(request)
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.register(form)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.post("/oauth2/renewal")
    async def renewal(request: Request) -> JSONResponse:
        try:
            _require_login(request, auth_service)
            return JSONResponse(status_code=200, content=ok(None, "续签成功"))
        except (AuthError, JwtError) as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))

    @router.post("/oauth2/doConfirm")
    @router.get("/oauth2/doConfirm")
    async def oauth_do_confirm(request: Request) -> JSONResponse:
        params = await _request_params(request)
        try:
            return JSONResponse(
                status_code=200,
                content=ok(await auth_service.confirm_qr_login(str(params.get("code") or ""), _bearer_token(request))),
            )
        except (AuthError, JwtError) as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))

    @router.get("/oauth2/thirdparty/{provider}/callback")
    async def thirdparty_callback(provider: str) -> JSONResponse:
        return JSONResponse(status_code=200, content=fail(None, "%s第三方登录通道未配置" % provider, 503))

    @router.get("/oauth2/callback")
    async def oauth_callback() -> JSONResponse:
        return JSONResponse(status_code=200, content=fail(None, "授权码登录通道未配置", 503))

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

    @router.get("/captcha/pkey")
    async def captcha_public_key(appKey: str = "") -> JSONResponse:
        public_key_value = await auth_service.public_key(appKey)
        if not public_key_value:
            return JSONResponse(status_code=200, content=fail(None, "客户端不存在或未配置公钥", 404))
        return JSONResponse(status_code=200, content=ok(public_key_value))

    @router.get("/captcha/vcode64")
    async def captcha_vcode64(width: int = 120, height: int = 40) -> JSONResponse:
        return JSONResponse(status_code=200, content=ok(await auth_service.captcha_base64(width, height)))

    @router.get("/captcha/verify")
    async def captcha_verify(vcode: str = "") -> JSONResponse:
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.verify_captcha(vcode)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(False, str(exc), exc.code))

    @router.get("/captcha/pcode")
    async def captcha_pcode(phone: str = "", vcode: str = "") -> JSONResponse:
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.send_phone_code(phone, vcode)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(False, str(exc), exc.code))

    @router.get("/captcha/pcode/verify")
    async def captcha_pcode_verify(phone: str = "", vcode: str = "") -> JSONResponse:
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.verify_phone_code(phone, vcode)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(False, str(exc), exc.code))

    @router.get("/qrcode/login")
    async def qrcode_login(
        client_id: str = "",
        redirect_uri: str = "",
        width: int = 200,
        height: int = 200,
    ) -> JSONResponse:
        try:
            return JSONResponse(
                status_code=200,
                content=ok(await auth_service.create_qr_login(client_id, redirect_uri, width, height)),
            )
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), exc.code))

    @router.get("/qrcode/check")
    async def qrcode_check(code: str = "") -> JSONResponse:
        try:
            state = await auth_service.qr_state(code)
            if isinstance(state, Mapping):
                return JSONResponse(status_code=200, content=ok(dict(state)))
            return JSONResponse(status_code=200, content=fail(state, "已扫描，待确认" if state == 1 else "请扫描", 202))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(0, str(exc), exc.code))

    @router.get("/qrcode/scanned")
    async def qrcode_scanned(code: str = "") -> JSONResponse:
        try:
            return JSONResponse(status_code=200, content=ok(await auth_service.mark_qr_scanned(code)))
        except AuthError as exc:
            return JSONResponse(status_code=200, content=fail(0, str(exc), exc.code))

    @router.post("/qrcode/confirm")
    @router.get("/qrcode/confirm")
    async def qrcode_confirm(request: Request) -> JSONResponse:
        params = await _request_params(request)
        try:
            return JSONResponse(
                status_code=200,
                content=ok(await auth_service.confirm_qr_login(str(params.get("code") or ""), _bearer_token(request))),
            )
        except (AuthError, JwtError) as exc:
            return JSONResponse(status_code=200, content=fail(None, str(exc), getattr(exc, "code", 401)))

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


def _authorize_login_html(params: Mapping[str, Any]) -> str:
    def field(name: str, default: str = "") -> str:
        return html.escape(str(params.get(name) or default), quote=True)

    client_id = field("client_id")
    redirect_uri = field("redirect_uri")
    scope = field("scope", "all")
    state = field("state")
    response_type = field("response_type", "code")
    return f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>JBM 认证中心</title>
  <style>
    body {{ margin: 0; min-height: 100vh; display: grid; place-items: center; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f8fafc; color: #0f172a; }}
    main {{ width: min(420px, calc(100vw - 32px)); border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; padding: 28px; box-shadow: 0 10px 30px rgba(15, 23, 42, .08); }}
    h1 {{ margin: 0 0 6px; font-size: 22px; }}
    p {{ margin: 0 0 18px; color: #64748b; font-size: 14px; }}
    label {{ display: block; margin: 14px 0 6px; font-size: 13px; font-weight: 600; }}
    input {{ box-sizing: border-box; width: 100%; height: 38px; border: 1px solid #cbd5e1; border-radius: 6px; padding: 0 10px; font-size: 14px; }}
    button {{ width: 100%; height: 40px; margin-top: 18px; border: 0; border-radius: 6px; background: #2563eb; color: #fff; font-weight: 600; cursor: pointer; }}
    button:disabled {{ opacity: .65; cursor: default; }}
    .client {{ margin-top: 14px; padding: 10px; border-radius: 6px; background: #f1f5f9; color: #475569; font-size: 12px; word-break: break-all; }}
    .error {{ display: none; margin-top: 12px; color: #dc2626; font-size: 13px; }}
  </style>
</head>
<body>
  <main>
    <h1>JBM 认证中心</h1>
    <p>登录后将授权当前应用并返回业务系统。</p>
    <form id="loginForm" method="post" action="./doLogin">
      <input type="hidden" name="response_type" value="{response_type}">
      <input type="hidden" name="client_id" value="{client_id}">
      <input type="hidden" name="redirect_uri" value="{redirect_uri}">
      <input type="hidden" name="scope" value="{scope}">
      <input type="hidden" name="state" value="{state}">
      <label for="username">用户名</label>
      <input id="username" name="username" autocomplete="username" required autofocus>
      <label for="password">密码</label>
      <input id="password" name="password" type="password" autocomplete="current-password" required>
      <button id="submitBtn" type="submit">登录并授权</button>
      <div id="error" class="error"></div>
      <div class="client">Client ID: {client_id or "未指定"}</div>
    </form>
  </main>
  <script>
    const form = document.getElementById('loginForm');
    const button = document.getElementById('submitBtn');
    const error = document.getElementById('error');
    form.addEventListener('submit', async (event) => {{
      event.preventDefault();
      error.style.display = 'none';
      button.disabled = true;
      button.textContent = '登录中...';
      try {{
        const response = await fetch(form.action, {{ method: 'POST', body: new FormData(form) }});
        const body = await response.json();
        if (body.success || body.code === 200) {{
          window.location.href = body.result;
          return;
        }}
        error.textContent = body.message || '登录失败';
        error.style.display = 'block';
      }} catch (e) {{
        error.textContent = '认证中心请求失败';
        error.style.display = 'block';
      }} finally {{
        button.disabled = false;
        button.textContent = '登录并授权';
      }}
    }});
  </script>
</body>
</html>"""


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
