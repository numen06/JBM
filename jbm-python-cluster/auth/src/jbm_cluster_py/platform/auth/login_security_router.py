from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.result import fail, ok
from jbm_cluster_py.common.api_access import is_platform
from jbm_cluster_py.common.admin_security import source_ip

from .jwt import JwtError
from .service import AuthError


def build_login_security_router(service):
    router = APIRouter(prefix="/oauth2/security")

    @router.get("/admin-policy")
    async def admin_policy(request: Request):
        header = request.headers.get("authorization", "")
        try:
            identity = await service.userinfo(header[7:] if header.lower().startswith("bearer ") else "")
            if not is_platform(identity):
                raise AuthError("仅平台管理员可查看管理接入策略", 403)
            policy = service.admin_access
            ip = source_ip(request.client.host if request.client else "", request.headers, policy.proxies)
            return JSONResponse(content=ok({"mode": policy.mode, "allowedNetworks": [str(v) for v in policy.networks],
                "currentSource": ip, "currentSourceAllowed": policy.evaluate(identity, ip),
                "passwordMaxAgeDays": int(service.password_policy.get("max-age-days") or 0),
                "roles": sorted(policy.roles)}), headers={"Cache-Control": "no-store"})
        except (AuthError, JwtError) as exc:
            code = getattr(exc, "code", 401)
            return JSONResponse(status_code=code, content=fail(None, str(exc), code))

    async def run(request, acknowledge=False):
        try:
            header = request.headers.get("authorization", "")
            token = header[7:] if header.lower().startswith("bearer ") else ""
            if not token:
                raise AuthError("请先登录用户账号", 401)
            identity = await service.userinfo(token)
            if identity.get("userId") is None:
                raise AuthError("请先登录用户账号", 401)
            user_id = int(identity["userId"])
            if acknowledge:
                raw = bytearray()
                async for chunk in request.stream():
                    raw.extend(chunk)
                    if len(raw) > 4096:
                        raise AuthError("请求过大", 413)
                import json

                payload = json.loads(raw)
                ids = payload.get("ids") if isinstance(payload, dict) else None
                if (
                    not isinstance(ids, list)
                    or not 1 <= len(ids) <= 20
                    or any(
                        isinstance(value, bool)
                        or not str(value).isdigit()
                        or len(str(value)) > 19
                        or not 0 < int(value) <= 9223372036854775807
                        for value in ids
                    )
                ):
                    raise AuthError("无效的登录提醒编号", 400)
                await service.repository.acknowledge_login_alerts(
                    user_id, [int(value) for value in ids]
                )
            result = await service.repository.login_alerts(user_id)
            return JSONResponse(content=ok(result), headers={"Cache-Control": "no-store"})
        except (AuthError, JwtError, ValueError, TypeError) as exc:
            code = getattr(exc, "code", 401 if isinstance(exc, JwtError) else 400)
            return JSONResponse(
                status_code=code,
                content=fail(None, str(exc), code),
                headers={"Cache-Control": "no-store"},
            )

    @router.get("/login-alerts")
    async def alerts(request: Request):
        return await run(request)

    @router.post("/login-alerts/acknowledge")
    async def acknowledge(request: Request):
        return await run(request, True)

    return router
