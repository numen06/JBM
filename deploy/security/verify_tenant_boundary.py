"""Read current grants and evaluate policy only. Never dispatch device/broker operations."""
import argparse
import asyncio
import json
from sqlalchemy import text
from fastapi import HTTPException
from jbm_cluster_py.common.api_access import is_super_admin
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.center.modules.governance.infrastructure.repository import SqlGovernanceRepository
from jbm_cluster_py.platform.center.modules.governance.application.request_access import RequestAccessService


async def main(args):
    repo = SqlGovernanceRepository(AppConfig.load(app='center').database)
    try:
        output = []
        async with repo.engine.connect() as db:
            for name in args.account:
                row = (await db.execute(text('SELECT user_id,user_name,company_id FROM base_user WHERE user_name=:name'), {'name': name})).mappings().first()
                if not row:
                    continue
                roles = await repo.user_roles(row['user_id'], args.app_id, row['company_id'])
                root = name == 'admin' or any(str(r.get('roleId', r.get('role_id'))) == '1' for r in roles)
                authorities = await repo.user_authorities(row['user_id'], root, args.app_id, row['company_id'])
                identity = {'userId': row['user_id'], 'username': name, 'tenantId': row['company_id'], 'appId': args.app_id,
                    'admin': root, 'roles': [r.get('roleCode', r.get('role_code')) for r in roles], 'permissions': [r['authority'] for r in authorities]}
                class CurrentIdentity:
                    async def authenticate(self, _):
                        return dict(identity)
                access = RequestAccessService(repo, None, CurrentIdentity())
                decisions = []
                for service, method, path in [
                    ('iot-platform', 'GET', '/emqx/clients'), ('iot-platform', 'DELETE', '/emqx/clients/verification-not-dispatched'),
                    ('iot-platform', 'POST', '/workers/rebalance'), ('iot-platform', 'GET', '/admin/users'),
                    ('iot-platform', 'GET', '/air-conditioning/connections'), ('iot-platform', 'GET', '/air-conditioning/systems'),
                    ('jbm-cluster-platform-center', 'GET', '/user'), ('jbm-cluster-platform-center', 'GET', '/role/all'),
                ]:
                    try:
                        await access.check({'service': service, 'method': method, 'path': path}, 'policy-verification-only')
                        status = 200
                    except HTTPException as exc:
                        status = exc.status_code
                    decisions.append({'path': path, 'method': method, 'status': status})
                if not is_super_admin(identity):
                    assert all(d['status'] == 403 for d in decisions[:5]), 'Infrastructure boundary failed'
                if 'tenant_admin' in identity['roles']:
                    assert all(d['status'] == 200 for d in decisions[-2:]), 'Own member API unavailable'
                menus = await repo.user_menus(row['user_id'], args.app_id, root, row['company_id'])
                output.append({'account': name, 'tenant': row['company_id'], 'roles': identity['roles'], 'root': is_super_admin(identity),
                    'decisions': decisions, 'rawMenuCount': len(menus)})
        print(json.dumps({'policyOnly': True, 'deviceControlDispatched': False, 'results': output}, ensure_ascii=False))
    finally:
        await repo.engine.dispose()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--account', action='append', required=True)
    p.add_argument('--app-id', type=int, required=True)
    asyncio.run(main(p.parse_args()))
