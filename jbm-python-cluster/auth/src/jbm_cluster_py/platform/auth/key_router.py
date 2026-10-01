from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from jbm_cluster_py.common.result import fail, ok

from .jwt import JwtError
from .service import AuthError


def build_key_router(service):
    router = APIRouter(prefix="/oauth2/keys")
    keys = service.key_login

    async def run(request: Request, action: str, key_id: str = ""):
        try:
            token = request.headers.get("authorization", "")
            token = token[7:] if token.lower().startswith("bearer ") else ""
            if request.method == "GET":
                result = await keys.list(token)
            elif request.method == "DELETE":
                result = await keys.remove(token, key_id)
            else:
                # Bound bodies before parsing; no private key material is accepted.
                raw = bytearray()
                async for chunk in request.stream():
                    raw.extend(chunk)
                    if len(raw) > 32768:
                        raise AuthError("密钥请求过大", 413)
                import json

                payload = json.loads(raw)
                if not isinstance(payload, dict):
                    raise ValueError("Invalid request")
                if action.endswith("options"):
                    address = request.client.host if request.client else "unknown"
                    if await service.cache.add_login_error("key-options:" + address, 1) > 60:
                        raise AuthError("请求过于频繁，请稍后再试", 429)
                if action == "login-options":
                    result = await keys.login_options(payload)
                elif action == "register-options":
                    result = await keys.registration_options(token, payload)
                else:
                    result = await keys.register(token, payload)
            return JSONResponse(content=ok(result), headers={"Cache-Control": "no-store"})
        except (AuthError, JwtError, ValueError, TypeError) as exc:
            code = getattr(exc, "code", 400)
            return JSONResponse(
                status_code=code,
                content=fail(None, str(exc), code),
                headers={"Cache-Control": "no-store"},
            )

    @router.post("/login-options")
    async def login_options(request: Request):
        return await run(request, "login-options")

    @router.post("/register-options")
    async def register_options(request: Request):
        return await run(request, "register-options")

    @router.post("/register")
    async def register(request: Request):
        return await run(request, "register")

    @router.get("")
    async def list_keys(request: Request):
        return await run(request, "list")

    @router.delete("/{key_id}")
    async def remove_key(key_id: str, request: Request):
        return await run(request, "remove", key_id)

    return router
