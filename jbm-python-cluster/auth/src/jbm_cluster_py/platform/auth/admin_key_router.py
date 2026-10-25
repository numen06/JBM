import json
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
from jbm_cluster_py.common.result import fail, ok
from .admin_keys import AdminKeys
from .jwt import JwtError
from .service import AuthError


def build_admin_key_router(auth):
    router = APIRouter(prefix='/oauth2/keys/admin/users')
    keys = AdminKeys(auth)

    async def run(request, user_id, action, key_id=''):
        token = request.headers.get('authorization', '')
        token = token[7:] if token.lower().startswith('bearer ') else ''
        try:
            from .login_security import add_login_context
            context = {}
            add_login_context(context, request)
            if action == 'list':
                result = await keys.list(token, user_id)
            elif action == 'remove':
                result = await keys.remove(token, user_id, key_id, context)
            else:
                raw = bytearray()
                async for chunk in request.stream():
                    raw.extend(chunk)
                    if len(raw) > 32768:
                        raise AuthError('密钥请求过大', 413)
                payload = json.loads(raw)
                if not isinstance(payload, dict):
                    raise ValueError('密钥请求无效')
                result = await keys.options(token, user_id, payload) if action == 'options' else await keys.register(token, user_id, payload, context)
            return JSONResponse(content=ok(result), headers={'Cache-Control': 'no-store'})
        except (AuthError, JwtError, ValueError, TypeError) as exc:
            code = getattr(exc, 'code', 400)
            return JSONResponse(status_code=code, content=fail(None, str(exc), code), headers={'Cache-Control': 'no-store'})

    @router.post('/{user_id}/register-options')
    async def options(user_id: int, request: Request):
        return await run(request, user_id, 'options')

    @router.post('/{user_id}/register')
    async def register(user_id: int, request: Request):
        return await run(request, user_id, 'register')

    @router.get('/{user_id}')
    async def list_keys(user_id: int, request: Request):
        return await run(request, user_id, 'list')

    @router.delete('/{user_id}/{key_id}')
    async def remove(user_id: int, key_id: str, request: Request):
        return await run(request, user_id, 'remove', key_id)

    return router
