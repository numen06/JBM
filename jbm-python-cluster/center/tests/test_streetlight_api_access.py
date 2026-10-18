import pytest
from fastapi import HTTPException
from jbm_cluster_py.common.api_access import authorize_api

READS = [('POST', '/streetlightArea/getList'), ('POST', '/businessRuleTemplate/getList'), ('POST', '/businessRuleInstance/getList'), ('GET', '/streetlightArea/12/illuminance-control')]
WRITES = [('POST', '/streetlightArea/saveData'), ('POST', '/businessRuleInstance/saveData'), ('PUT', '/streetlightArea/12/illuminance-control')]
ROOT = {'userId': 1, 'admin': True}
READER = {'userId': 2, 'tenantId': 20, 'permissions': ['MENU_iot_streetlights', 'iot.platform.read']}
OPERATOR = {**READER, 'permissions': READER['permissions'] + ['iot.platform.operate']}

@pytest.mark.parametrize('method,path', READS + WRITES)
def test_root_and_authorized_streetlight_operator_can_use_registered_family(method, path):
    for identity in [ROOT, OPERATOR]:
        authorize_api('iot-platform', method, path, identity)

@pytest.mark.parametrize('method,path', READS)
def test_streetlight_reader_can_open_area_dependencies(method, path):
    authorize_api('iot-platform', method, path, READER)

@pytest.mark.parametrize('method,path', WRITES)
def test_reader_cannot_change_control(method, path):
    with pytest.raises(HTTPException): authorize_api('iot-platform', method, path, READER)

@pytest.mark.parametrize('method,path', READS + WRITES)
def test_generic_iot_grants_do_not_grant_streetlight_business(method, path):
    with pytest.raises(HTTPException):
        authorize_api('iot-platform', method, path, {'userId': 3, 'permissions': ['MENU_iot_air_conditioning', 'iot.platform.read', 'iot.platform.operate']})

def test_business_operator_cannot_modify_shared_template_or_access_infrastructure():
    for method,path in [('POST', '/businessRuleTemplate/saveData'), ('GET', '/workers'), ('GET', '/emqx/clients')]:
        with pytest.raises(HTTPException): authorize_api('iot-platform', method, path, OPERATOR)
    with pytest.raises(HTTPException): authorize_api('iot-platform', 'GET', '/unknown-api', ROOT)
