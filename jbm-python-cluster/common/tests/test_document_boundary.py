"""Regression checks against both service assemblies and configurable middleware."""
import importlib
import httpx
import pytest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import FastAPI, HTTPException

from jbm_cluster_py.common.api_access import ApiRule, authorize_api
from jbm_cluster_py.common.auth import UserInfoAuthClient, install_api_protection
from jbm_cluster_py.common.config import AppConfig


@pytest.mark.parametrize("service", ["push", "bigscreen", "doc", "job", "logs", "gateway", "auth", "center"])
@pytest.mark.asyncio
async def test_actual_service_document_routes_deny_anonymous_even_with_legacy_whitelist(service):
    module = importlib.import_module(
        "jbm_cluster_py.platform.center.bootstrap.app" if service == "center"
        else f"jbm_cluster_py.platform.{service}.main"
    )
    data = {
        "spring": {"application": {"name": "jbm-cluster-platform-" + service},
                   "datasource": {"url": "sqlite+aiosqlite:///:memory:"}},
        "jbm": {
            "api-access": {"public-paths": ["/docs", "/redoc", "/openapi.json"]},
            service: {"security": {"public-paths": ["/docs", "/redoc", "/openapi.json"]}},
        },
    }
    if service == "auth":
        key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        data["jbm"]["auth"]["jwt"] = {
            "private-key": key.private_bytes(serialization.Encoding.PEM,
                serialization.PrivateFormat.PKCS8, serialization.NoEncryption()).decode(),
            "issuer": "https://auth.test.invalid",
        }
    config = AppConfig(data, profile="prod", config_dir=None, app=service)
    app = module.create_app(config)
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://direct-service") as client:
        for path in [app.openapi_url, app.docs_url, app.redoc_url]:
            response = await client.get(path)
            assert response.status_code == 401, (service, path, response.text[:100])


@pytest.mark.asyncio
async def test_custom_documents_bypass_neither_prefix_filter_nor_wide_public_directory(monkeypatch):
    identities = {
        "Bearer user": {"userId": 42, "permissions": ["API_schema"], "roles": ["tenant_admin"]},
        "Bearer admin": {"userId": 1, "admin": True},
        "Bearer service": {"clientId": "gateway", "scope": "internal"},
    }
    calls = []

    async def broker(self, url, *, json, headers):
        calls.append(json)
        identity = identities[headers["Authorization"]]
        try:
            authorize_api(json["service"], json["method"], json["path"], identity)
        except HTTPException as exc:
            return httpx.Response(exc.status_code, json={"success": False})
        return httpx.Response(200, json={"result": {"allowed": True, "identity": identity}})

    monkeypatch.setattr(httpx.AsyncClient, "post", broker)
    config = AppConfig({"jbm": {"api-access": {"public-paths": ["/reference/"]}}},
                       profile="prod", config_dir=None, app="gateway")
    app = FastAPI(docs_url="/reference/ui", redoc_url="/reference/redoc", openapi_url="/reference/schema")
    install_api_protection(app, config, protected_prefixes=("/gateway",))
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://direct-service") as client:
        for path in [app.openapi_url, app.docs_url, app.redoc_url, "/docs/oauth2-redirect"]:
            assert (await client.get(path)).status_code == 401
            assert (await client.get(path, headers={"Authorization": "Bearer user"})).status_code == 403
        for token in ["Bearer admin", "Bearer service"]:
            assert (await client.get(app.openapi_url, headers={"Authorization": token})).status_code == 200
    assert {c["path"] for c in calls} <= {"/openapi.json", "/docs", "/redoc", "/docs/oauth2-redirect"}


@pytest.mark.parametrize("path", ["/docs/", "/v3/api-docs/swagger-config", "/swagger-ui/index.html"])
def test_metadata_rules_override_service_specific_rules(path):
    with pytest.raises(HTTPException) as exc:
        authorize_api("push", "GET", path, {"userId": 42}, extra_rules=(ApiRule(path, authenticated=True),))
    assert exc.value.status_code == 403


@pytest.mark.asyncio
async def test_production_cannot_disable_guard_or_make_everything_anonymous():
    async with httpx.AsyncClient() as client:
        auth = UserInfoAuthClient({"enabled": False}, client)
        with pytest.raises(ValueError, match="生产环境"):
            auth.bind_app(FastAPI(), profile="prod")
        for paths in [["/"], ["/**"], "/docs"]:
            with pytest.raises(ValueError):
                UserInfoAuthClient({"public-paths": paths}, client)
        auth = UserInfoAuthClient({"public-paths": []}, client, default_public_paths=("/health",))
        assert not auth.is_public("/health")
