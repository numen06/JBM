"""Super-administrator issuance of account-bound public-key credentials."""
from __future__ import annotations

from datetime import UTC, datetime
import hashlib
import json
import time

from sqlalchemy import delete, insert, select
from sqlalchemy.exc import IntegrityError

from .key_login import credentials, credential_expiration, file_key_id, file_public_key, six_months_after, verify_file_signature
from .repository import _insert_available
from .service import AuthError, _account_version


class AdminKeys:
    def __init__(self, auth):
        self.auth = auth
        self.keys = auth.key_login

    async def actor(self, token):
        self.keys.require_enabled()
        actor = await self.auth.userinfo(token)
        if actor.get('admin') is not True or actor.get('userId') is None or actor.get('mustChangePassword'):
            raise AuthError('仅超级管理员可签发或撤销用户登录密钥', 403)
        if not await self.auth.repository.has_table('base_account_logs'):
            raise AuthError('密钥操作审计存储不可用', 503)
        columns, _ = await self.auth.repository._login_columns()
        if not {'risk_detail', 'credential_id'} <= columns or not {'login_type', 'account_type'}.intersection(columns):
            raise AuthError('请先完成登录安全审计数据库迁移', 503)
        return actor

    async def target(self, user_id):
        user = await self.auth.repository.find_user(int(user_id))
        if user is None:
            raise AuthError('用户不存在', 404)
        return user

    async def options(self, token, user_id, payload):
        actor = await self.actor(token)
        account, user = await self.keys.account(int(user_id))
        if 'privateKey' in payload or payload.get('kind', 'FILE_KEY') != 'FILE_KEY':
            raise AuthError('管理员签发仅接受文件登录公钥', 400)
        public_key = file_public_key(payload.get('publicKey'))
        name = str(payload.get('name') or '').strip()
        reason = str(payload.get('reason') or '').strip()
        if not name or len(name) > 80 or not reason or len(reason) > 250:
            raise AuthError('请填写密钥名称和签发用途', 400)
        validity = payload.get('validity', 'six_months')
        if validity not in {'six_months', 'permanent'}:
            raise AuthError('请选择半年或长期有效', 400)
        created = int(time.time())
        state = await self.keys.challenge({
            'purpose': 'admin-register', 'kind': 'FILE_KEY', 'userId': int(user_id),
            'issuedBy': int(actor['userId']), 'issuedByAccount': str(actor['username']),
            'session': hashlib.sha256(token.encode()).hexdigest(), 'name': name,
            'reason': reason, 'publicKey': public_key, 'accountVersion': _account_version(account),
            'createdAt': created, 'expiresAt': six_months_after(created) if validity == 'six_months' else None,
        })
        return {**self.keys.file_options(state), 'account': account['account'], 'userId': str(user_id),
                'keyId': file_key_id(public_key), 'createdAt': created, 'expiresAt': state['expiresAt']}

    async def register(self, token, user_id, payload, context):
        actor = await self.actor(token)
        if 'privateKey' in payload:
            raise AuthError('服务端不接收私钥', 400)
        state = await self.keys.consume(payload, 'admin-register', 'FILE_KEY')
        if (state['userId'] != int(user_id) or state['issuedBy'] != int(actor['userId'])
                or state['session'] != hashlib.sha256(token.encode()).hexdigest()):
            raise AuthError('管理员签发会话与目标账号不匹配', 401)
        account, user = await self.keys.account(int(user_id))
        if state['accountVersion'] != _account_version(account):
            raise AuthError('账号凭证已更新，请重新签发', 409)
        try:
            verify_file_signature(state['publicKey'], self.keys.file_options(state)['message'], payload['signature'])
        except Exception as exc:
            raise AuthError('公钥持有证明失败', 401) from exc
        key_id = file_key_id(state['publicKey'])
        data = {'publicKey': state['publicKey'], 'accountVersion': _account_version(account),
                'expiresAt': state['expiresAt'], 'issuedBy': state['issuedBy'],
                'issuedByAccount': state['issuedByAccount'], 'issuanceReason': state['reason']}
        lock = 'credential-name:' + str(user_id) + ':' + hashlib.sha256(state['name'].casefold().encode()).hexdigest()
        try:
            async with self.keys.cache.lock(lock):
                async with self.keys.engine.begin() as conn:
                    names = (await conn.execute(select(credentials.c.name).where(credentials.c.user_id == int(user_id)))).scalars().all()
                    if len(names) >= 20 or any(str(n).strip().casefold() == state['name'].casefold() for n in names):
                        raise AuthError('密钥已达到上限或名称已存在，请检查后再签发', 409)
                    await conn.execute(insert(credentials).values(id=key_id, user_id=int(user_id), kind='FILE_KEY',
                        name=state['name'], data=json.dumps(data), sign_count=0, created_at=state['createdAt']))
                    await self.audit(conn, actor, user, key_id, 'ADMIN_KEY_ISSUE', state['name'], state['reason'], context)
        except IntegrityError as exc:
            raise AuthError('该公钥已绑定账号', 409) from exc
        return {'id': key_id, 'userId': str(user_id), 'account': account['account'], 'name': state['name'], 'expiresAt': state['expiresAt']}

    async def list(self, token, user_id):
        await self.actor(token)
        await self.target(user_id)
        async with self.keys.engine.connect() as conn:
            rows = (await conn.execute(select(credentials).where(credentials.c.user_id == int(user_id)))).mappings().all()
        return [{'id': r['id'], 'kind': r['kind'], 'name': r['name'], 'created_at': r['created_at'],
                 'last_used_at': r['last_used_at'], 'expires_at': credential_expiration(r),
                 'issuedBy': json.loads(r['data']).get('issuedByAccount')} for r in rows]

    async def remove(self, token, user_id, key_id, context):
        actor = await self.actor(token)
        user = await self.target(user_id)
        async with self.keys.cache.lock('credential:' + key_id):
            async with self.keys.engine.begin() as conn:
                row = (await conn.execute(select(credentials).where(credentials.c.id == key_id,
                    credentials.c.user_id == int(user_id)).with_for_update())).mappings().first()
                if row is None:
                    raise AuthError('此用户没有该密钥', 404)
                await conn.execute(delete(credentials).where(credentials.c.id == key_id, credentials.c.user_id == int(user_id)))
                await self.audit(conn, actor, user, key_id, 'ADMIN_KEY_REVOKE', row['name'], '管理员撤销', context)
        return {'id': key_id, 'userId': str(user_id), 'revoked': True}

    async def audit(self, conn, actor, target, key_id, action, name, reason, context):
        now = datetime.now(UTC).replace(tzinfo=None)
        detail = {'action': action, 'actorUserId': str(actor['userId']), 'targetUserId': str(target['user_id']),
                  'targetTenantId': str(target.get('company_id') or ''), 'targetAccount': target.get('user_name'),
                  'keyId': key_id, 'keyName': name, 'reason': reason}
        await _insert_available(conn, 'base_account_logs', {
            'user_id': int(actor['userId']), 'account': str(actor['username']), 'login_type': action, 'account_type': action,
            'login_time': now, 'create_time': now, 'update_time': now, 'status': 1, 'login_status': 1,
            'message': ('签发' if action == 'ADMIN_KEY_ISSUE' else '撤销') + '用户 ' + str(target.get('user_name')) + ' 的登录密钥',
            'ip': str(context.get('login_ip') or '')[:64], 'login_ip': str(context.get('login_ip') or '')[:64],
            'user_agent': str(context.get('user_agent') or '')[:512], 'login_agent': str(context.get('user_agent') or '')[:512],
            'credential_id': key_id, 'request_id': context.get('request_id'), 'client_id': actor.get('clientId'),
            'risk_level': 'info', 'risk_detail': json.dumps(detail, ensure_ascii=False),
        })
