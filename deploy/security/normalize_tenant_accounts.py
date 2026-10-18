"""Run in Center. Dry-run first; --apply writes only role/menu links, never business data.

Accounts are implementation input, not customer-specific application logic.
The private JSON backup includes exact before rows and must be retained for rollback.
"""
import argparse
import asyncio
import json
import secrets
import time
from pathlib import Path
from sqlalchemy import text
from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.center.modules.governance.infrastructure.repository import SqlGovernanceRepository


def new_id():
    return time.time_ns() // 1000 + secrets.randbelow(1000)


async def main(args):
    repo = SqlGovernanceRepository(AppConfig.load(app='center').database)
    try:
        async with repo.engine.begin() as db:
            async def rows(sql, **values):
                return [dict(r) for r in (await db.execute(text(sql), values)).mappings()]
            users = []
            for name in dict.fromkeys(args.demote + args.tenant_admin):
                found = await rows('SELECT user_id,user_name,user_type,company_id FROM base_user WHERE user_name=:name', name=name)
                if not found:
                    continue
                user = found[0]
                assert name != 'admin' and user['company_id'], 'Refuse root or unowned account'
                user['roles'] = await rows('SELECT * FROM base_role_user WHERE user_id=:u', u=user['user_id'])
                users.append(user)
            roles = await rows('SELECT role_id,role_code,app_id FROM base_role')
            root_ids = {r['role_id'] for r in roles if r['role_id'] == 1 or r['role_code'] in {'super_admin', 'platform_operator', 'iot_operator'}}
            tenant_roles = [r for r in roles if r['role_code'] == 'tenant_admin' and r['app_id'] is None]
            assert len(tenant_roles) == 1
            role_id = tenant_roles[0]['role_id']
            old_grants = await rows('SELECT * FROM base_authority_role WHERE role_id=:r', r=role_id)
            plan = {'accounts': users, 'tenantAdminRole': role_id, 'tenantAdminGrantsBefore': old_grants}
            backup = Path(args.backup)
            if args.apply:
                assert not backup.exists(), 'Use a fresh backup path for each apply'
                backup.write_text(json.dumps(plan, default=str, ensure_ascii=False), encoding='utf-8')
                backup.chmod(0o600)
            changes = []
            for user in users:
                uid, tenant, name = user['user_id'], user['company_id'], user['user_name']
                if name in args.demote:
                    removed = [r for r in user['roles'] if r['role_id'] in root_ids]
                    if args.apply:
                        for link in removed:
                            await db.execute(text('DELETE FROM base_role_user WHERE id=:id AND user_id=:u'), {'id': link['id'], 'u': uid})
                        await db.execute(text('UPDATE base_role_user SET tenant_id=:t,update_time=NOW() WHERE user_id=:u AND tenant_id IS NULL'), {'u': uid, 't': tenant})
                    changes.append({'account': name, 'tenant': tenant, 'removedPrivilegedLinks': len(removed), 'businessLinksRetained': len(user['roles']) - len(removed)})
                if name in args.tenant_admin:
                    apps = await rows('SELECT app_id FROM base_tenant_app WHERE tenant_id=:t AND status=1', t=tenant)
                    for app in apps:
                        aid = app['app_id']
                        linked = any(r['role_id'] == role_id and r['tenant_id'] == tenant and r['app_id'] in (None, aid) for r in user['roles'])
                        if args.apply and not linked:
                            await db.execute(text('INSERT INTO base_role_user(id,user_id,role_id,tenant_id,app_id,create_time,update_time) VALUES(:id,:u,:r,:t,:a,NOW(),NOW())'), {'id': new_id(), 'u': uid, 'r': role_id, 't': tenant, 'a': aid})
                        # Standard member menu; no global role/API/platform configuration menus.
                        menu = await rows("SELECT menu_id FROM base_menu WHERE app_id=:a AND menu_code='users'", a=aid)
                        mid = menu[0]['menu_id'] if menu else new_id()
                        authority = await rows("SELECT authority_id FROM base_authority WHERE app_id=:a AND authority='MENU_users'", a=aid)
                        authid = authority[0]['authority_id'] if authority else new_id()
                        if args.apply:
                            if not menu:
                                await db.execute(text("INSERT INTO base_menu(menu_id,menu_code,menu_name,path,app_id,status,priority) VALUES(:id,'users','成员管理','/system/users',:a,1,80)"), {'id': mid, 'a': aid})
                            if not authority:
                                await db.execute(text("INSERT INTO base_authority(authority_id,authority,resource_type,menu_id,app_id,status) VALUES(:id,'MENU_users','MENU',:m,:a,1)"), {'id': authid, 'm': mid, 'a': aid})
                            if not await rows('SELECT id FROM base_authority_role WHERE role_id=:r AND authority_id=:id', r=role_id, id=authid):
                                await db.execute(text('INSERT INTO base_authority_role(id,role_id,authority_id,app_id) VALUES(:id,:r,:g,:a)'), {'id': new_id(), 'r': role_id, 'g': authid, 'a': aid})
                    changes.append({'account': name, 'tenant': tenant, 'tenantAdminApps': [a['app_id'] for a in apps]})
            print(json.dumps({'applied': args.apply, 'changes': changes}, ensure_ascii=False))
    finally:
        await repo.engine.dispose()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--demote', action='append', default=[])
    p.add_argument('--tenant-admin', action='append', default=[])
    p.add_argument('--backup', required=True)
    p.add_argument('--apply', action='store_true')
    asyncio.run(main(p.parse_args()))
