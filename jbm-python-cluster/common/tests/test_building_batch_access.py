import pytest
from fastapi import HTTPException
from jbm_cluster_py.common.api_access import authorize_api


def user(*permissions):
    return {'userId': 42, 'tenantId': 7, 'permissions': list(permissions)}


def test_recount_uses_existing_calculation_permission_and_report_readers_cannot_trigger_it():
    service = 'smart-building-data-batch'
    authorize_api(service, 'POST', '/batch/reCount', user(
        'MENU_building_calculation_completeness', 'building.platform.operate'))
    authorize_api(service, 'GET', '/batch/reCountJobs/test-job', user('MENU_building_reports'))
    for identity in [user(), user('MENU_building_reports'), user('MENU_building_history'),
                     user('MENU_building_calculation_completeness'), user('building.platform.operate')]:
        with pytest.raises(HTTPException) as exc:
            authorize_api(service, 'POST', '/batch/reCount', identity)
        assert exc.value.status_code == 403
    with pytest.raises(HTTPException):
        authorize_api(service, 'POST', '/batch/unregistered', user('building.platform.operate'))
