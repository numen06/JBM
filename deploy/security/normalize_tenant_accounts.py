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
    write = args.apply or args.rehearse
    repo = SqlGovernanceRepository(AppConfig.load(app='center').database)
    try:
        async with repo.engine.begin() as db:
            async def rows(sql, **values):
                return [dict(r) for r in (await db.execute(text(sql), values)).mappings()]
            grant_has_id = any(c['Field'] == 'id' for c in await rows('SHOW COLUMNS FROM base_authority_role'))
            async def link_grant(role, authority):
                if await rows('SELECT authority_id FROM base_authority_role WHERE role_id=:r AND authority_id=:g', r=role, g=authority):
                    return
                columns, values = ('id,', ':id,') if grant_has_id else ('', '')
                await db.execute(text('INSERT INTO base_authority_role(' + columns + 'role_id,authority_id,app_id,create_time) VALUES(' + values + ':r,:g,NULL,NOW())'), {'id': new_id(), 'r': role, 'g': authority})
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
                    if write:
                        for link in removed:
                            await db.execute(text('DELETE FROM base_role_user WHERE id=:id AND user_id=:u'), {'id': link['id'], 'u': uid})
                        await db.execute(text('UPDATE base_role_user SET tenant_id=:t,update_time=NOW() WHERE user_id=:u AND tenant_id IS NULL'), {'u': uid, 't': tenant})
                    changes.append({'account': name, 'tenant': tenant, 'removedPrivilegedLinks': len(removed), 'businessLinksRetained': len(user['roles']) - len(removed)})
                if name in args.tenant_admin:
                    apps = await rows('SELECT app_id FROM base_tenant_app WHERE tenant_id=:t AND status=1', t=tenant)
                    for app in apps:
                        aid = app['app_id']
                        linked = any(r['role_id'] == role_id and r['tenant_id'] == tenant and r['app_id'] in (None, aid) for r in user['roles'])
                        if write and not linked:
                            await db.execute(text('INSERT INTO base_role_user(id,user_id,role_id,tenant_id,app_id,create_time,update_time) VALUES(:id,:u,:r,:t,:a,NOW(),NOW())'), {'id': new_id(), 'u': uid, 'r': role_id, 't': tenant, 'a': aid})
                        # Standard member menu; no global role/API/platform configuration menus.
                        menu = await rows("SELECT menu_id FROM base_menu WHERE app_id IS NULL AND menu_code='users'")
                        mid = menu[0]['menu_id'] if menu else new_id()
                        authority = await rows("SELECT authority_id FROM base_authority WHERE app_id IS NULL AND authority='MENU_users'")
                        authid = authority[0]['authority_id'] if authority else new_id()
                        if write:
                            if not menu:
                                await db.execute(text("INSERT INTO base_menu(menu_id,menu_code,menu_name,path,scheme,service_id,app_id,status,priority,create_time) VALUES(:id,'users','成员管理','/system/users','/','jbm-cluster-platform-center',:a,1,80,NOW())"), {'id': mid, 'a': None})
                            if not authority:
                                await db.execute(text("INSERT INTO base_authority(authority_id,authority,resource_type,menu_id,app_id,status) VALUES(:id,'MENU_users','menu',:m,:a,1)"), {'id': authid, 'm': mid, 'a': None})
                            await link_grant(role_id, authid)
                            for action in ('users_add', 'users_edit', 'users_delete'):
                                code = 'ACTION_' + action
                                found = await rows('SELECT authority_id FROM base_authority WHERE app_id IS NULL AND authority=:code', code=code)
                                grant = found[0]['authority_id'] if found else new_id()
                                if not found:
                                    await db.execute(text("INSERT INTO base_authority(authority_id,authority,resource_type,menu_id,app_id,status) VALUES(:id,:code,'action',:m,:a,1)"), {'id': grant, 'code': code, 'm': mid, 'a': None})
                                await link_grant(role_id, grant)
                    changes.append({'account': name, 'tenant': tenant, 'tenantAdminApps': [a['app_id'] for a in apps]})
            if args.rehearse:
                await db.rollback()
            print(json.dumps({'applied': args.apply, 'rehearsedAndRolledBack': args.rehearse, 'changes': changes}, ensure_ascii=False))
    finally:
        await repo.engine.dispose()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--demote', action='append', default=[])
    p.add_argument('--tenant-admin', action='append', default=[])
    p.add_argument('--backup', required=True)
    mode = p.add_mutually_exclusive_group()
    mode.add_argument('--apply', action='store_true')
    mode.add_argument('--rehearse', action='store_true', help='Execute SQL then roll back the entire transaction')
    asyncio.run(main(p.parse_args()))
