import time

import pytest
from fastapi import HTTPException
from jbm_cluster_py.platform.center.modules.governance.application.request_access import RequestAccessService


@pytest.mark.parametrize('service,method,path', [
    ('jbm-cluster-platform-center', 'PUT', '/user/123/password'),
    ('jbm-cluster-platform-center', 'POST', '/role/save'),
    ('iot-platform', 'POST', '/device/batchControl'),
])
def test_sensitive_operation_requires_fresh_authentication_even_with_rotated_access(service, method, path):
    identity = {'userId': 7, 'authTime': int(time.time()) - 601, 'recentAuthSeconds': 600}
    with pytest.raises(HTTPException) as exc:
        RequestAccessService.require_recent_authentication(service, method, path, identity)
    assert exc.value.status_code == 401
    identity['authTime'] = int(time.time())
    RequestAccessService.require_recent_authentication(service, method, path, identity)


def test_polling_reads_and_ordinary_control_do_not_require_repeated_login():
    old = {'userId': 7, 'authTime': 1}
    RequestAccessService.require_recent_authentication('jbm-cluster-platform-center', 'POST', '/user/pageList', old)
    RequestAccessService.require_recent_authentication('iot-platform', 'POST', '/air-conditioning/control', old)
