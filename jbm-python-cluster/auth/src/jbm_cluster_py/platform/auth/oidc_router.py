"""HTTP and browser ceremony for the optional standard issuer."""

from __future__ import annotations

import hmac
import html
import secrets
import time
from collections.abc import Mapping
from typing import Any
from urllib.parse import urlencode, urlsplit

import jwt as pyjwt
from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse, Response
from fastapi.routing import APIRoute
from redis.exceptions import RedisError
from sqlalchemy.exc import SQLAlchemyError

from jbm_cluster_py.platform.auth.jwt import JwtError
from jbm_cluster_py.platform.auth.oidc import OidcError, OidcService, digest
from jbm_cluster_py.platform.auth.service import AuthError

SSO_COOKIE = "__Host-jbm_oidc_sso"
BIND_COOKIE = "__Host-jbm_oidc_tx"
HEADERS = {
    "Cache-Control": "no-store",
    "Pragma": "no-cache",
    "Referrer-Policy": "no-referrer",
    "X-Content-Type-Options": "nosniff",
    "Content-Security-Policy": (
        "default-src 'none'; img-src data:; form-action 'self'; "
        "frame-ancestors 'none'; base-uri 'none'"
    ),
}


def failure(exc: OidcError) -> JSONResponse:
    headers = dict(HEADERS)
    if exc.status == 401:
        headers["WWW-Authenticate"] = (
            'Basic realm="oidc"'
            if exc.error == "invalid_client"
            else 'Bearer error="invalid_token"'
        )
    return JSONResponse(
        {"error": exc.error, "error_description": exc.description},
        status_code=exc.status,
        headers=headers,
    )


def unique_params(items: Any) -> dict[str, str]:
    result = {}
    for key, value in items:
        if key in result or not isinstance(value, str) or len(value) > 8192:
            raise OidcError("invalid_request", "Duplicate, oversized or non-text parameter")
        result[key] = value
    return result


async def form_params(request: Request) -> dict[str, str]:
    if request.headers.get("content-type", "").split(";")[0] != "application/x-www-form-urlencoded":
        raise OidcError("invalid_request", "Expected form-urlencoded request")
    body = await request.body()
    if len(body) > 32768:
        raise OidcError("invalid_request", "Request too large")
    return unique_params((await request.form()).multi_items())


def cookie(response: Response, name: str, value: str, age: int) -> None:
    response.set_cookie(
        name, value, max_age=age, secure=True, httponly=True, samesite="lax", path="/"
    )


def redirect(uri: str) -> RedirectResponse:
    return RedirectResponse(uri, status_code=303, headers=HEADERS)


class OidcRoute(APIRoute):
    def get_route_handler(self):
        original = super().get_route_handler()

        async def handler(request: Request) -> Response:
            try:
                return await original(request)
            except (AuthError, RedisError, SQLAlchemyError):
                return failure(OidcError("temporarily_unavailable", status=503))

        return handler


def build_oidc_router(auth_service: Any, config: Mapping[str, Any]) -> APIRouter:
    router = APIRouter(prefix="/oidc", tags=["oidc"], route_class=OidcRoute)
    if str((config.get("oidc") or {}).get("enabled", False)).strip().lower() not in {
        "true",
        "1",
        "yes",
        "on",
    }:
        return router
    service = OidcService(auth_service, config)
    router.oidc_service = service

    async def page(request: Request, p: Mapping[str, Any], stage: str, sid: str = "") -> Response:
        tx, csrf = secrets.token_urlsafe(32), secrets.token_urlsafe(32)
        bind = request.cookies.get(BIND_COOKIE) or secrets.token_urlsafe(32)
        await service.cache.set_json(
            "oidc:tx:" + digest(tx),
            {"request": dict(p), "stage": stage, "sid": sid, "csrf": csrf, "bind": digest(bind)},
            600,
        )
        hidden = (
            '<input type="hidden" name="transaction" value="' + tx + '">'
            '<input type="hidden" name="csrf" value="' + csrf + '">'
        )
        fields = ""
        if stage == "login":
            title = "登录 JBM"
            fields = (
                '<label>账号 <input name="username" autocomplete="username" required></label>'
                '<label>密码 <input type="password" name="password" '
                'autocomplete="current-password" required></label>'
            )
            if auth_service.login_captcha_required:
                captcha = await auth_service.captcha_base64()
                fields += (
                    '<img alt="验证码" src="' + html.escape(captcha, quote=True) + '">'
                    '<label>验证码 <input name="vcode" required></label>'
                )
        elif stage == "consent":
            title = "允许应用访问"
            fields = (
                "<p>应用："
                + html.escape(p["client_id"])
                + "</p><p>权限："
                + html.escape(" ".join(p["scopes"]))
                + "</p>"
            )
        else:
            title = "退出 JBM 登录"
            fields = "<p>确认结束当前 JBM 登录会话及其应用令牌？</p>"
        action = "/oidc/logout" if stage == "logout" else "/oidc/continue"
        content = (
            '<!doctype html><html lang="zh-CN"><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width,initial-scale=1">'
            "<title>" + title + "</title><main><h1>" + title + "</h1>"
            '<form method="post" action="'
            + action
            + '">'
            + hidden
            + fields
            + '<button name="decision" value="approve">确认</button>'
            '<button name="decision" value="deny" formnovalidate>取消</button>'
            "</form></main></html>"
        )
        page_headers = dict(HEADERS)
        # Chrome derives navigation-form Origin from the page referrer policy.
        # no-referrer yields Origin:null, while same-origin preserves our strict
        # form-origin check without disclosing authorization URLs to the RP.
        page_headers["Referrer-Policy"] = "same-origin"
        target = p.get("post_logout_redirect_uri") if stage == "logout" else p.get("redirect_uri")
        if target:
            parsed = urlsplit(target)
            # Browsers can enforce form-action on the final 303 redirect as well.
            # The target is already matched to an exact registered client URI.
            origin = parsed.scheme + "://" + parsed.netloc
            page_headers["Content-Security-Policy"] = HEADERS["Content-Security-Policy"].replace(
                "form-action 'self';", "form-action 'self' " + origin + ";"
            )
        response = HTMLResponse(content, headers=page_headers)
        cookie(response, BIND_COOKIE, bind, 600)
        return response

    async def transaction(request: Request, form: dict[str, str]) -> tuple[str, dict[str, Any]]:
        key = "oidc:tx:" + digest(form.get("transaction", ""))
        tx = await service.cache.get_json(key)
        if (
            not tx
            or not hmac.compare_digest(tx["csrf"].encode(), form.get("csrf", "").encode())
            or not hmac.compare_digest(tx["bind"], digest(request.cookies.get(BIND_COOKIE, "")))
        ):
            raise OidcError("invalid_request", "Invalid or expired browser transaction")
        origin = request.headers.get("origin")
        expected = service.issuer.removesuffix("/oidc")
        if origin and origin != expected:
            raise OidcError("invalid_request", "Invalid form origin")
        return key, tx

    async def advance(
        request: Request, p: dict[str, Any], client: dict[str, Any], sid: str
    ) -> Response:
        state = await service.session(sid)
        if state:
            if int(time.time()) - state["auth_time"] >= service.sso_seconds:
                state = None
            age = p.get("max_age_value")
            if (
                state
                and age is not None
                and (age == 0 or int(time.time()) - state["auth_time"] > age)
            ):
                state = None
        if state:
            await service.identity(state, client)
        if not state or "login" in p["prompts"]:
            if "none" in p["prompts"]:
                return redirect(service.redirect(p, error="login_required"))
            return await page(request, p, "login")
        if await service.consent_needed(sid, p, client):
            if "none" in p["prompts"]:
                return redirect(service.redirect(p, error="consent_required"))
            return await page(request, p, "consent", sid)
        return redirect(await service.code(sid, p, client))

    @router.get("/.well-known/openid-configuration")
    async def discovery() -> dict[str, Any]:
        return service.metadata()

    @router.get("/jwks")
    async def jwks() -> dict[str, Any]:
        return service.signer.jwks()

    @router.api_route("/authorize", methods=["GET", "POST"])
    async def authorize(request: Request) -> Response:
        p = None
        try:
            if request.method == "POST":
                if request.query_params:
                    raise OidcError("invalid_request", "Mixed query and form parameters")
                p = await form_params(request)
            else:
                p = unique_params(request.query_params.multi_items())
            client = await service.client(p.get("client_id", ""))
            # No redirect or state echo until the exact registered target is trusted.
            if p.get("redirect_uri") not in client["oidc"]["redirectUris"]:
                raise OidcError("invalid_request", "Unregistered redirect_uri")
        except OidcError as exc:
            return failure(exc)
        try:
            p = service.validate_request(p, client)
            return await advance(request, p, client, request.cookies.get(SSO_COOKIE, ""))
        except OidcError as exc:
            return redirect(service.redirect(p, error=exc.error))

    @router.post("/continue")
    async def continue_authorization(request: Request) -> Response:
        try:
            form = await form_params(request)
            key, _ = await transaction(request, form)
            async with service.cache.lock(key):
                _, tx = await transaction(request, form)
                if tx["stage"] not in {"login", "consent"}:
                    raise OidcError("invalid_request")
                p, sid = tx["request"], tx["sid"]
                client = await service.client(p["client_id"])
                if p["redirect_uri"] not in client["oidc"]["redirectUris"]:
                    raise OidcError("invalid_request")
                if form.get("decision") != "approve":
                    await service.cache.delete(key)
                    return redirect(service.redirect(p, error="access_denied"))
                if tx["stage"] == "login":
                    credentials = {
                        key: form.get(key, "") for key in ("username", "password", "vcode")
                    }
                    credentials["loginType"] = "PASSWORD"
                    try:
                        account, user, _ = await auth_service._authenticate_user_for_client(
                            client, credentials
                        )
                    except AuthError as exc:
                        if exc.code >= 500:
                            raise
                        await auth_service._record_login(
                            credentials, status=0, message="OIDC credential validation failed"
                        )
                        raise OidcError(
                            "access_denied", "Login failed; start a new authorization request"
                        ) from None
                    if account.get(
                        "must_change_password"
                    ) or not await service.repo.oidc_user_context(int(user["user_id"]), client):
                        raise OidcError(
                            "access_denied", "Account is not eligible for this application"
                        )
                    old_sid = request.cookies.get(SSO_COOKIE, "")
                    sid, _ = await service.create_session(account, user)
                    if old_sid:
                        await service.cache.delete("oidc:sso:" + digest(old_sid))
                    await auth_service._record_login(credentials)
                    p["prompts"] = [value for value in p["prompts"] if value != "login"]
                    # max_age=0 was fulfilled by the credential check just completed.
                    p["max_age_value"] = None
                    await service.cache.delete(key)
                    response = await advance(request, p, client, sid)
                    cookie(response, SSO_COOKIE, sid, service.sso_seconds)
                    return response
                if request.cookies.get(SSO_COOKIE, "") != sid:
                    raise OidcError("invalid_request", "Login session changed")
                state = await service.session(sid)
                if not state:
                    raise OidcError("login_required")
                await service.identity(state, client)
                await service.approve(sid, p)
                await service.cache.delete(key)
                return redirect(await service.code(sid, p, client))
        except OidcError as exc:
            return failure(exc)

    @router.post("/token")
    async def token(request: Request) -> Response:
        try:
            form = await form_params(request)
            client = await service.authenticate_client(
                form, request.headers.get("authorization", "")
            )
            return JSONResponse(await service.exchange(form, client), headers=HEADERS)
        except OidcError as exc:
            return failure(exc)

    @router.api_route("/userinfo", methods=["GET", "POST"])
    async def userinfo(request: Request) -> Response:
        try:
            authorization = request.headers.get("authorization", "")
            if not authorization.lower().startswith("bearer ") or request.query_params:
                raise OidcError("invalid_token", status=401)
            return JSONResponse(await service.userinfo(authorization[7:]), headers=HEADERS)
        except OidcError as exc:
            return failure(exc)

    @router.post("/revoke")
    async def revoke(request: Request) -> Response:
        try:
            form = await form_params(request)
            client = await service.authenticate_client(
                form, request.headers.get("authorization", "")
            )
            if not form.get("token"):
                raise OidcError("invalid_request")
            await service.revoke(form["token"], client)
            return Response(status_code=200, headers=HEADERS)
        except OidcError as exc:
            return failure(exc)

    @router.post("/introspect")
    async def introspect(request: Request) -> Response:
        try:
            form = await form_params(request)
            client = await service.authenticate_client(
                form, request.headers.get("authorization", "")
            )
            if not form.get("token"):
                raise OidcError("invalid_request")
            return JSONResponse(await service.introspect(form["token"], client), headers=HEADERS)
        except OidcError as exc:
            return failure(exc)

    @router.get("/logout")
    async def logout_page(request: Request) -> Response:
        try:
            p = unique_params(request.query_params.multi_items())
            if set(p) - {"id_token_hint", "client_id", "post_logout_redirect_uri", "state"}:
                raise OidcError("invalid_request")
            client_id = p.get("client_id", "")
            hint = None
            if p.get("id_token_hint"):
                # Audience is established by signature-validated claims, never a redirect target.
                try:
                    candidate = pyjwt.decode(
                        p["id_token_hint"], options={"verify_signature": False}
                    )
                    if not isinstance(candidate.get("aud"), str):
                        raise OidcError("invalid_request", "Invalid ID token audience")
                    await service.client(candidate["aud"])
                    hint = service.signer.verify(
                        p["id_token_hint"], audience=candidate["aud"], typ="JWT", verify_exp=False
                    )
                except (JwtError, pyjwt.PyJWTError) as exc:
                    raise OidcError("invalid_request", "Invalid id_token_hint") from exc
                if not isinstance(hint.get("aud"), str) or "auth_time" not in hint:
                    raise OidcError("invalid_request", "Invalid id_token_hint")
                if client_id and client_id != hint["aud"]:
                    raise OidcError("invalid_request")
                client_id = hint["aud"]
            if p.get("post_logout_redirect_uri"):
                client = await service.client(client_id)
                if (
                    not hint
                    or p["post_logout_redirect_uri"] not in client["oidc"]["postLogoutRedirectUris"]
                ):
                    raise OidcError(
                        "invalid_request", "Unregistered logout redirect or missing ID token hint"
                    )
            sid = request.cookies.get(SSO_COOKIE, "")
            if hint and hint.get("sid") != digest(sid):
                raise OidcError(
                    "invalid_request", "ID token does not match the current browser session"
                )
            return await page(request, p, "logout", sid)
        except OidcError as exc:
            return failure(exc)

    @router.post("/logout")
    async def logout(request: Request) -> Response:
        try:
            form = await form_params(request)
            key, _ = await transaction(request, form)
            async with service.cache.lock(key):
                _, tx = await transaction(request, form)
                if tx["stage"] != "logout" or tx["sid"] != request.cookies.get(SSO_COOKIE, ""):
                    raise OidcError("invalid_request")
                await service.cache.delete(key)
                if form.get("decision") != "approve":
                    return HTMLResponse("<p>已取消退出。</p>", headers=HEADERS)
                await service.cache.delete("oidc:sso:" + digest(tx["sid"]))
                target = tx["request"].get("post_logout_redirect_uri")
                if target:
                    params = {"state": tx["request"]["state"]} if "state" in tx["request"] else {}
                    response = redirect(
                        target
                        + (
                            ("&" if urlsplit(target).query else "?") + urlencode(params)
                            if params
                            else ""
                        )
                    )
                else:
                    response = HTMLResponse("<p>已退出登录。</p>", headers=HEADERS)
                response.delete_cookie(
                    SSO_COOKIE, path="/", secure=True, httponly=True, samesite="lax"
                )
                return response
        except OidcError as exc:
            return failure(exc)

    return router
