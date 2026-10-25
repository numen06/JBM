import asyncio
import json
from urllib.parse import parse_qs, urlsplit

import pytest
from sqlalchemy import text
from test_key_login import client as key_client, login, VERIFIER
from test_file_key import public, signed, proof, key

client = key_client


def seed_targets(client):
    async def run():
        async with client.app.state.auth_service.repository.engine.begin() as conn:
            for user_id in [7, 8]:
                await conn.execute(text("INSERT INTO base_user (user_id,user_name,company_id,status) VALUES (:id,:name,100,1)"), {'id': user_id, 'name': f'operator{user_id}'})
                await conn.execute(text("INSERT INTO base_account (account_id,user_id,account,password,account_type,status,domain,must_change_password) VALUES (:id,:id,:name,'operator123','username',1,'@admin.com',0)"), {'id': user_id, 'name': f'operator{user_id}'})
            await conn.execute(text("INSERT INTO base_tenant_app (tenant_id,app_id,status) VALUES (100,1000,1)"))
    asyncio.run(run())


def options(client, key, user_id=7):
    response = client.post(f'/oauth2/keys/admin/users/{user_id}/register-options', json={
        'name': '运维登录密钥', 'reason': '已核对租户成员，开通多联机运维登录', 'publicKey': public(key)})
    assert response.status_code == 200, response.text
    return response.json()['result']


def issue(client, key, user_id=7):
    data = options(client, key, user_id)
    response = client.post(f'/oauth2/keys/admin/users/{user_id}/register', json={
        'challengeId': data['challengeId'], 'signature': signed(key, data['message'])})
    assert response.status_code == 200, response.text
    return response.json()['result']


def token_from_proof(client, key):
    response = login(client, proof(client, key), kind='FILE_KEY')
    assert response.status_code == 200, response.text
    code = parse_qs(urlsplit(response.json()['result']).query)['code'][0]
    response = client.post('/oauth2/token', data={'grant_type': 'authorization_code', 'client_id': 'JBM',
        'code': code, 'redirect_uri': 'http://admin.test/login/callback', 'code_verifier': VERIFIER})
    assert response.status_code == 200, response.text
    return response.json()['access_token']


def test_admin_issues_real_file_key_without_target_login_and_revoke_is_immediate(client, key):
    seed_targets(client)
    issued = issue(client, key)
    token = token_from_proof(client, key)
    headers = {'Authorization': 'Bearer ' + token}
    who = client.get('/oauth2/userinfo', headers=headers).json()['result']
    assert who['username'] == 'operator7' and who['admin'] is False and who['tenantId'] == 100
    assert not who['permissions']  # A key never adds roles or permissions.
    denied = client.post('/oauth2/keys/admin/users/8/register-options', headers=headers,
                         json={'name': 'illegal', 'reason': 'illegal', 'publicKey': public(key)})
    assert denied.status_code == 403
    assert client.delete(f'/oauth2/keys/admin/users/8/{issued["id"]}').status_code == 404
    assert client.delete(f'/oauth2/keys/admin/users/7/{issued["id"]}').status_code == 200
    assert client.get('/oauth2/userinfo', headers=headers).status_code == 401
    assert login(client, proof(client, key), kind='FILE_KEY').status_code == 401

    async def audit():
        async with client.app.state.auth_service.repository.engine.connect() as conn:
            rows = (await conn.execute(text("SELECT login_type,risk_detail FROM base_account_logs WHERE login_type LIKE 'ADMIN_KEY_%' ORDER BY rowid"))).all()
            assert [r[0] for r in rows] == ['ADMIN_KEY_ISSUE', 'ADMIN_KEY_REVOKE']
            for _, raw in rows:
                detail = json.loads(raw)
                assert detail['targetUserId'] == '7' and detail['keyId'] == issued['id']
                assert 'PRIVATE' not in raw and 'PUBLIC KEY' not in raw
    asyncio.run(audit())


def test_admin_challenge_cannot_change_user_or_be_replayed_and_rejects_private_keys(client, key):
    seed_targets(client)
    data = options(client, key)
    body = {'challengeId': data['challengeId'], 'signature': signed(key, data['message'])}
    assert client.post('/oauth2/keys/admin/users/8/register', json=body).status_code == 401
    assert client.post('/oauth2/keys/admin/users/7/register', json=body).status_code == 401
    assert client.get('/oauth2/keys/admin/users/7').json()['result'] == []
    denied = client.post('/oauth2/keys/admin/users/7/register-options', json={
        'name': 'invalid', 'reason': 'test', 'publicKey': public(key), 'privateKey': 'must never be submitted'})
    assert denied.status_code == 400
    assert 'must never' not in denied.text


def test_admin_credential_and_audit_are_atomic(client, key, monkeypatch):
    seed_targets(client)
    from jbm_cluster_py.platform.auth import admin_keys
    async def unavailable(*args, **kwargs):
        raise RuntimeError('audit unavailable')
    monkeypatch.setattr(admin_keys, '_insert_available', unavailable)
    data = options(client, key)
    with pytest.raises(RuntimeError, match='audit unavailable'):
        client.post('/oauth2/keys/admin/users/7/register', json={
            'challengeId': data['challengeId'], 'signature': signed(key, data['message'])})
    assert client.get('/oauth2/keys/admin/users/7').json()['result'] == []
