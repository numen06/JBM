import importlib.util
import json
from pathlib import Path
from types import SimpleNamespace

import pytest
import sqlalchemy as sa
from fastapi import HTTPException

from jbm_cluster_py.common.api_access import authorize_api, is_platform
from jbm_cluster_py.platform.center.modules.governance.application.access import require_tenant_record

MIGRATIONS = Path(__file__).parents[1] / 'migrations'
DEFINITION = json.loads((MIGRATIONS / 'sql/builtin_operations.json').read_text(encoding='utf-8'))


def test_operations_permissions_keep_platform_and_other_tenants_protected():
    identity = {'userId': 20, 'tenantId': 1, 'appId': 30, 'roles': ['operations'],
                'permissions': ['MENU_' + code for code in DEFINITION['menuCodes']]
                + ['ACTION_' + code for code in DEFINITION['operationCodes']]}
    assert not is_platform(identity)
    with pytest.raises(HTTPException):
        require_tenant_record(identity, {'companyId': 2}, 'companyId')
    for service, method, path in [
        ('building', 'GET', '/api/v1/building/overview'),
        ('building', 'POST', '/api/v1/building/master-data/device-bindings'),
        ('iot', 'GET', '/air-conditioning/systems'),
        ('iot', 'POST', '/air-conditioning/commands'),
    ]:
        authorize_api(service, method, path, identity)
    for service, method, path in [
        ('center', 'POST', '/user'), ('center', 'POST', '/role'),
        ('center', 'PUT', '/authority/roles/1'), ('job', 'POST', '/sysJob/database-backups/backups'),
        ('iot', 'GET', '/workers'), ('iot', 'POST', '/catalog/products'),
    ]:
        with pytest.raises(HTTPException):
            authorize_api(service, method, path, identity)


def test_migration_installs_default_once_without_assigning_accounts():
    spec = importlib.util.spec_from_file_location('builtin_operations', MIGRATIONS / 'versions/20261103_28_builtin_operations.py')
    migration = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(migration)
    engine = sa.create_engine('sqlite://')
    metadata = sa.MetaData()
    definitions = {
        'base_role': [('role_id', sa.BigInteger), ('role_code', sa.String), ('role_name', sa.String), ('role_desc', sa.String), ('is_persist', sa.Integer)],
        'base_menu': [('menu_id', sa.BigInteger), ('app_id', sa.BigInteger), ('menu_code', sa.String)],
        'base_action': [('action_id', sa.BigInteger), ('app_id', sa.BigInteger), ('menu_id', sa.BigInteger), ('action_code', sa.String), ('action_name', sa.String), ('is_persist', sa.Integer)],
        'base_authority': [('authority_id', sa.BigInteger), ('app_id', sa.BigInteger), ('menu_id', sa.BigInteger), ('action_id', sa.BigInteger), ('authority', sa.String), ('resource_type', sa.String)],
        'base_authority_role': [('id', sa.BigInteger), ('authority_id', sa.BigInteger), ('role_id', sa.BigInteger)],
    }
    for name, columns in definitions.items():
        sa.Table(name, metadata, *[sa.Column(key, kind, primary_key=index == 0) for index, (key, kind) in enumerate(columns)],
                 sa.Column('status', sa.Integer), sa.Column('create_time', sa.DateTime), sa.Column('update_time', sa.DateTime))
    metadata.create_all(engine)
    with engine.begin() as conn:
        conn.execute(metadata.tables['base_menu'].insert(), {'menu_id': 10, 'app_id': 30, 'menu_code': 'building_group_energy', 'status': 1})
        conn.execute(metadata.tables['base_authority'].insert(), [
            {'authority_id': 11, 'authority': 'MENU_building_dashboard', 'status': 1},
            {'authority_id': 12, 'authority': 'ACTION_iot.platform.admin', 'status': 1},
            {'authority_id': 13, 'authority': 'MENU_roles', 'status': 1},
            {'authority_id': 14, 'authority': 'MENU_iot_diagnostics', 'status': 1},
            {'authority_id': 15, 'authority': 'ACTION_iot.platform.operate', 'status': 0},
        ])
        migration.op = SimpleNamespace(get_bind=lambda: conn)
        migration.upgrade()
        migration.upgrade()
        role = conn.execute(sa.select(metadata.tables['base_role'])).mappings().one()
        assert role['role_code'] == 'operations' and role['is_persist'] == 1
        granted = set(conn.execute(sa.select(metadata.tables['base_authority'].c.authority).join(
            metadata.tables['base_authority_role'], metadata.tables['base_authority'].c.authority_id == metadata.tables['base_authority_role'].c.authority_id)).scalars())
        assert granted == {'MENU_building_dashboard', 'ACTION_building.platform.operate'}
        assert conn.execute(sa.select(sa.func.count()).select_from(metadata.tables['base_action'])).scalar() == 1
