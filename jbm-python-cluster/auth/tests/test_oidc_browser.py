"""Opt-in real Chromium ceremony against temporary, local-only HTTPS servers.

Run with JBM_AUTH_BROWSER_TEST=1 and Playwright + its Chromium installed.
JBM_AUTH_CHROMIUM_EXECUTABLE can select an existing Chromium executable explicitly.
Certificate verification is disabled only in this test's temporary browser context
because the local fixture generates a self-signed certificate; production is unchanged.
"""

from __future__ import annotations

import asyncio
import base64
import hashlib
import ipaddress
import json
import os
import socket
from contextlib import asynccontextmanager, contextmanager
from datetime import UTC, datetime, timedelta
from pathlib import Path
from urllib.parse import urlencode, urlsplit

import bcrypt
import jwt
import pytest
import uvicorn
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
from jbm_cluster_py.integrations.redis import RedisClient
from jbm_cluster_py.platform.auth.oidc_router import SSO_COOKIE, build_oidc_router
from jbm_cluster_py.platform.auth.repository import AuthRepository
from jbm_cluster_py.platform.auth.service import AuthService, TokenCache
from sqlalchemy import text

pytestmark = pytest.mark.skipif(
    os.environ.get("JBM_AUTH_BROWSER_TEST") != "1",
    reason="Set JBM_AUTH_BROWSER_TEST=1 for the optional real Chromium HTTPS smoke test",
)


def _certificate(tmp_path: Path) -> tuple[Path, Path]:
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "JBM local browser test")])
    now = datetime.now(UTC)
    cert = (
        x509.CertificateBuilder()
        .subject_name(name)
        .issuer_name(name)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now - timedelta(minutes=1))
        .not_valid_after(now + timedelta(days=1))
        .add_extension(
            x509.SubjectAlternativeName(
                [x509.DNSName("localhost"), x509.IPAddress(ipaddress.ip_address("127.0.0.1"))]
            ),
            critical=False,
        )
        .sign(key, hashes.SHA256())
    )
    cert_path, key_path = tmp_path / "local-test.crt", tmp_path / "local-test.key"
    cert_path.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    key_path.write_bytes(
        key.private_bytes(
            serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8,
            serialization.NoEncryption(),
        )
    )
    return cert_path, key_path


class _LocalServer(uvicorn.Server):
    @contextmanager
    def capture_signals(self):
        # Tests own their event loop and must not replace pytest's signal handlers.
        yield


@asynccontextmanager
async def _https_server(app: FastAPI, cert: Path, key: Path):
    listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    listener.bind(("127.0.0.1", 0))
    listener.setblocking(False)
    port = listener.getsockname()[1]
    server = _LocalServer(
        uvicorn.Config(
            app,
            host="127.0.0.1",
            port=port,
            ssl_certfile=str(cert),
            ssl_keyfile=str(key),
            log_level="error",
            access_log=False,
            lifespan="off",
            timeout_graceful_shutdown=2,
        )
    )
    task = asyncio.create_task(server.serve(sockets=[listener]))
    try:
        async with asyncio.timeout(10):
            while not server.started:
                if task.done():
                    await task
                    raise RuntimeError("Local HTTPS test server did not start")
                await asyncio.sleep(0.01)
        yield port
    finally:
        server.should_exit = True
        try:
            await asyncio.wait_for(task, timeout=10)
        finally:
            listener.close()


async def _seed(repo: AuthRepository, rp_origin: str) -> None:
    async with repo.engine.begin() as conn:
        for app_id, client in enumerate(("rp-a", "rp-b"), 1):
            oidc = {
                "enabled": True,
                "redirectUris": [f"{rp_origin}/{client}/callback"],
                "postLogoutRedirectUris": [f"{rp_origin}/{client}/logout"],
                "scopes": ["openid", "profile", "offline_access"],
                "audiences": [f"{rp_origin}/{client}/api"],
                "tokenEndpointAuthMethod": "none",
            }
            await conn.execute(
                text(
                    "INSERT INTO base_app (app_id, api_key, status, extend_data) "
                    "VALUES (:app_id, :client, 1, :data)"
                ),
                {"app_id": app_id, "client": client, "data": json.dumps({"oidc": oidc})},
            )
            await conn.execute(
                text("INSERT INTO base_tenant_app (tenant_id, app_id, status) VALUES (10, :id, 1)"),
                {"id": app_id},
            )
            await conn.execute(
                text(
                    "INSERT INTO base_role_user (user_id, role_id, app_id, tenant_id) "
                    "VALUES (7, 5, :id, 10)"
                ),
                {"id": app_id},
            )
        await conn.execute(
            text(
                "INSERT INTO base_user (user_id, user_name, real_name, company_id, status) "
                "VALUES (7, 'alice', 'Alice', 10, 1)"
            )
        )
        await conn.execute(
            text(
                "INSERT INTO base_account (account_id, user_id, account, password, account_type, "
                "status, domain, must_change_password) "
                "VALUES (7, 7, 'alice', :password, 'username', 1, '@admin.com', 0)"
            ),
            {"password": bcrypt.hashpw(b"test-password", bcrypt.gensalt()).decode()},
        )
        await conn.execute(text("INSERT INTO base_org (id, status) VALUES (10, 1)"))
        await conn.execute(text("INSERT INTO base_user_org (user_id, org_id) VALUES (7, 10)"))
        await conn.execute(
            text("INSERT INTO base_role (role_id, role_code, status) VALUES (5, 'member', 1)")
        )


@pytest.mark.asyncio
async def test_chromium_cross_origin_login_sso_and_confirmed_logout(tmp_path):
    from playwright.async_api import async_playwright, expect

    cert, key = _certificate(tmp_path)
    repo = AuthRepository({"url": f"sqlite+aiosqlite:///{tmp_path / 'browser.db'}"})
    await repo.start()
    rp = FastAPI()
    callbacks: dict[str, dict[str, str]] = {}
    logout_requests: list[dict[str, str]] = []
    rp_cookie_headers: list[str] = []
    rp_referrers: list[str] = []

    @rp.get("/{client}/callback")
    async def callback(client: str, request: Request):
        callbacks[client] = dict(request.query_params)
        rp_cookie_headers.append(request.headers.get("cookie", ""))
        rp_referrers.append(request.headers.get("referer", ""))
        return HTMLResponse("<h1>RP callback received</h1>")

    @rp.get("/{client}/logout")
    async def logged_out(client: str, request: Request):
        logout_requests.append({"client": client, **dict(request.query_params)})
        rp_cookie_headers.append(request.headers.get("cookie", ""))
        rp_referrers.append(request.headers.get("referer", ""))
        return HTMLResponse("<h1>RP logout received</h1>")

    try:
        async with _https_server(rp, cert, key) as rp_port:
            rp_origin = f"https://127.0.0.1:{rp_port}"
            await _seed(repo, rp_origin)
            idp = FastAPI()
            form_origins: list[str] = []

            @idp.middleware("http")
            async def record_form_origin(request: Request, call_next):
                if request.method == "POST" and request.url.path == "/oidc/continue":
                    form_origins.append(request.headers.get("origin", ""))
                return await call_next(request)

            async with _https_server(idp, cert, key) as idp_port:
                idp_origin = f"https://localhost:{idp_port}"
                issuer = idp_origin + "/oidc"
                config = {"oidc": {"enabled": True, "issuer": issuer}}
                auth = AuthService(repo, TokenCache(RedisClient({"enabled": False})), config)
                idp.include_router(build_oidc_router(auth, config))
                async with async_playwright() as playwright:
                    chromium = await playwright.chromium.launch(
                        headless=True,
                        executable_path=os.environ.get("JBM_AUTH_CHROMIUM_EXECUTABLE"),
                    )
                    # Scoped to these generated local test certificates, never app configuration.
                    context = await chromium.new_context(ignore_https_errors=True)
                    page = await context.new_page()
                    page.set_default_timeout(15000)
                    security_errors = []
                    page.on(
                        "console",
                        lambda message: (
                            security_errors.append(message.text)
                            if message.type == "error" and "Content Security Policy" in message.text
                            else None
                        ),
                    )
                    tokens: dict[str, dict] = {}
                    sso_sid = None
                    try:
                        discovery = await (
                            await context.request.get(issuer + "/.well-known/openid-configuration")
                        ).json()
                        jwks = await (await context.request.get(discovery["jwks_uri"])).json()
                        public_key = jwt.PyJWK.from_dict(jwks["keys"][0]).key
                        for client in ("rp-a", "rp-b"):
                            verifier = ("a" if client == "rp-a" else "b") * 64
                            challenge = (
                                base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
                                .decode()
                                .rstrip("=")
                            )
                            request_params = {
                                "client_id": client,
                                "redirect_uri": f"{rp_origin}/{client}/callback",
                                "response_type": "code",
                                "scope": "openid profile offline_access",
                                "state": "browser-state-" + client,
                                "nonce": "browser-nonce-" + client,
                                "code_challenge": challenge,
                                "code_challenge_method": "S256",
                            }
                            response = await page.goto(
                                discovery["authorization_endpoint"]
                                + "?"
                                + urlencode(request_params)
                            )
                            assert rp_origin in response.headers["content-security-policy"]
                            if client == "rp-a":
                                await expect(page.locator('input[name="username"]')).to_be_visible()
                                await page.locator('input[name="username"]').fill("alice")
                                await page.locator('input[name="password"]').fill("test-password")
                                await page.get_by_role("button", name="确认", exact=True).click()
                                await page.wait_for_load_state("load")
                                assert "允许应用访问" in await page.locator("body").inner_text(), (
                                    await page.locator("body").inner_text(),
                                    form_origins,
                                )
                            else:
                                await expect(page.locator('input[name="username"]')).to_have_count(
                                    0
                                )
                            await expect(
                                page.get_by_role("heading", name="允许应用访问")
                            ).to_be_visible()
                            await page.get_by_role("button", name="确认", exact=True).click()
                            await page.wait_for_url(f"{rp_origin}/{client}/callback?*")
                            await expect(
                                page.get_by_role("heading", name="RP callback received")
                            ).to_be_visible()
                            assert callbacks[client]["state"] == request_params["state"]
                            assert callbacks[client]["iss"] == issuer
                            cookie = next(
                                item
                                for item in await context.cookies(idp_origin)
                                if item["name"] == SSO_COOKIE
                            )
                            assert cookie["secure"] and cookie["httpOnly"]
                            assert cookie["sameSite"] == "Lax" and cookie["path"] == "/"
                            assert cookie["domain"] == "localhost"
                            if sso_sid is None:
                                sso_sid = cookie["value"]
                            else:
                                assert cookie["value"] == sso_sid
                            exchanged = await context.request.post(
                                discovery["token_endpoint"],
                                form={
                                    "grant_type": "authorization_code",
                                    "client_id": client,
                                    "code": callbacks[client]["code"],
                                    "redirect_uri": request_params["redirect_uri"],
                                    "code_verifier": verifier,
                                },
                            )
                            assert exchanged.status == 200
                            tokens[client] = await exchanged.json()
                            claims = jwt.decode(
                                tokens[client]["id_token"],
                                public_key,
                                algorithms=["RS256"],
                                audience=client,
                                issuer=issuer,
                            )
                            assert claims["nonce"] == request_params["nonce"]
                        assert set(callbacks) == {"rp-a", "rp-b"}
                        assert form_origins and all(origin == idp_origin for origin in form_origins)
                        assert all(SSO_COOKIE not in header for header in rp_cookie_headers)
                        response = await page.goto(
                            discovery["end_session_endpoint"]
                            + "?"
                            + urlencode(
                                {
                                    "id_token_hint": tokens["rp-a"]["id_token"],
                                    "post_logout_redirect_uri": f"{rp_origin}/rp-a/logout",
                                    "state": "browser-logout",
                                }
                            )
                        )
                        assert rp_origin in response.headers["content-security-policy"]
                        await expect(
                            page.get_by_role("heading", name="退出 JBM 登录")
                        ).to_be_visible()
                        await page.get_by_role("button", name="确认", exact=True).click()
                        await page.wait_for_url(f"{rp_origin}/rp-a/logout?state=browser-logout")
                        assert logout_requests == [{"client": "rp-a", "state": "browser-logout"}]
                        assert not any(rp_referrers), "Authorization parameters leaked to an RP"
                        assert not any(
                            item["name"] == SSO_COOKIE for item in await context.cookies(idp_origin)
                        )
                        for client, token in tokens.items():
                            info = await context.request.get(
                                discovery["userinfo_endpoint"],
                                headers={"Authorization": "Bearer " + token["access_token"]},
                            )
                            assert info.status == 401
                            refresh = await context.request.post(
                                discovery["token_endpoint"],
                                form={
                                    "grant_type": "refresh_token",
                                    "client_id": client,
                                    "refresh_token": token["refresh_token"],
                                },
                            )
                            assert refresh.status == 400
                        assert not security_errors, security_errors
                        assert urlsplit(page.url).hostname == "127.0.0.1"
                    finally:
                        await context.close()
                        await chromium.close()
    finally:
        await repo.stop()
