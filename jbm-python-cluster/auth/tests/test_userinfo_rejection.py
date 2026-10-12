import json

import pytest
from starlette.requests import Request

from jbm_cluster_py.platform.auth.router import _userinfo
from jbm_cluster_py.platform.auth.service import AuthError
from jbm_cluster_py.platform.auth.jwt import JwtError


@pytest.mark.parametrize('error,status', [
    (AuthError('登录会话已失效', 401), 401),
    (AuthError('租户应用授权已失效', 403), 403),
    (AuthError('backend unavailable', 503), 503),
    (JwtError('invalid_issuer'), 401),
])
async def test_userinfo_preserves_denial_status_and_logs_no_credential(error, status, caplog):
    credential = 'test-secret-credential'
    request = Request({'type':'http', 'headers':[(b'authorization', ('Bearer '+credential).encode())],
                       'query_string':b''})
    class Auth:
        async def userinfo(self, token):
            assert token == credential
            raise error
    response = await _userinfo(request, Auth())
    assert response.status_code == status
    assert json.loads(response.body)['code'] == status
    assert str(error) in caplog.text
    assert credential not in caplog.text and credential not in response.body.decode()
