import asyncio
import json
import secrets
import base64
from urllib.parse import parse_qs, urlsplit

import pytest
from sqlalchemy import text

from jbm_cluster_py.platform.auth.encrypted_file_key import EncryptedFileKey
from test_key_login import client as key_client, login, VERIFIER
from test_file_key import encrypted_password
from test_admin_keys import seed_targets

client = key_client


@pytest.fixture(autouse=True)
def platform_encryption_key(monkeypatch):
    monkeypatch.setenv('JBM_FILE_KEY_ENCRYPTION_KEY', base64.urlsafe_b64encode(secrets.token_bytes(32)).decode())


def issue(client, admin=False):
    if admin:
        prefix = '/oauth2/keys/admin/users/7'
    else:
        prefix = '/oauth2/keys'
        assert client.post(prefix + '/verify-password', json={'password': encrypted_password(client)}).status_code == 200
    options = client.post(prefix + '/register-options', json={
        'kind': 'FILE_KEY', 'fileVersion': 2, 'name': '加密密钥', 'reason': '运维账号交付'})
    assert options.status_code == 200, options.text
    options = options.json()['result']
    assert options['fileVersion'] == 2
    result = client.post(prefix + '/register', json={'kind': 'FILE_KEY', 'challengeId': options['challengeId']})
    assert result.status_code == 200, result.text
    file = result.json()['result']
    assert file['version'] == 2 and not {'publicKey', 'privateKey'} & file.keys()
    return file


def proof(client, file):
    options = client.post('/oauth2/keys/login-options', json={'kind': 'FILE_KEY', 'client_id': 'JBM'})
    assert options.status_code == 200
    return {'challengeId': options.json()['result']['challengeId'], 'fileKey': file['payload']}


def exchange(client, response):
    assert response.status_code == 200, response.text
    code = parse_qs(urlsplit(response.json()['result']).query)['code'][0]
    result = client.post('/oauth2/token', data={'grant_type': 'authorization_code', 'client_id': 'JBM',
        'code': code, 'redirect_uri': 'http://admin.test/login/callback', 'code_verifier': VERIFIER})
    assert result.status_code == 200, result.text
    return result.json()['access_token']


def test_cipher_is_authenticated_platform_bound_and_survives_restart():
    master = secrets.token_bytes(32)
    cipher = EncryptedFileKey(master, 'admin.test')
    key_id, data, payload = cipher.issue()
    assert EncryptedFileKey(master, 'admin.test').open(payload) == (key_id, data['secretHash'])
    damaged = payload[:60] + ('A' if payload[60] != 'A' else 'B') + payload[61:]
    for other, value in [(cipher, damaged), (EncryptedFileKey(master, 'other.test'), payload),
                         (EncryptedFileKey(secrets.token_bytes(32), 'admin.test'), payload)]:
        with pytest.raises(ValueError):
            other.open(value)


def test_missing_platform_key_refuses_encrypted_issuance(client, monkeypatch):
    monkeypatch.delenv('JBM_FILE_KEY_ENCRYPTION_KEY')
    assert client.post('/oauth2/keys/verify-password', json={'password': encrypted_password(client)}).status_code == 200
    response = client.post('/oauth2/keys/register-options', json={'kind': 'FILE_KEY', 'fileVersion': 2, 'name': 'not-issued'})
    assert response.status_code == 503
    assert client.get('/oauth2/keys').json()['result'] == []


def test_encrypted_file_full_oauth_replay_and_revocation(client):
    file = issue(client)
    payload = proof(client, file)
    token = exchange(client, login(client, payload, 'FILE_KEY'))
    client.headers['Authorization'] = 'Bearer ' + token
    assert client.get('/oauth2/userinfo').json()['result']['username'] == 'admin'
    assert login(client, payload, 'FILE_KEY').status_code == 401
    damaged = dict(file, payload=file['payload'][:70] + ('A' if file['payload'][70] != 'A' else 'B') + file['payload'][71:])
    assert login(client, proof(client, damaged), 'FILE_KEY').status_code == 401
    assert client.delete('/oauth2/keys/' + file['keyId']).status_code == 200
    assert login(client, proof(client, file), 'FILE_KEY').status_code == 401


@pytest.mark.parametrize('mutation', ['password', 'disabled', 'expired'])
def test_encrypted_file_keeps_account_and_expiry_checks(client, mutation):
    file = issue(client)
    async def change():
        async with client.app.state.auth_service.repository.engine.begin() as conn:
            if mutation == 'password':
                result = await conn.execute(text("UPDATE base_account SET password='changed' WHERE account='admin'"))
                assert result.rowcount == 1
            elif mutation == 'disabled':
                result = await conn.execute(text("UPDATE base_user SET status=0 WHERE user_name='admin'"))
                assert result.rowcount == 1
            else:
                raw = (await conn.execute(text('SELECT data FROM base_auth_credential WHERE id=:id'), {'id': file['keyId']})).scalar_one()
                data = json.loads(raw); data['expiresAt'] = 1
                await conn.execute(text('UPDATE base_auth_credential SET data=:data WHERE id=:id'), {'data': json.dumps(data), 'id': file['keyId']})
    asyncio.run(change())
    denied = login(client, proof(client, file), 'FILE_KEY')
    assert denied.status_code == (403 if mutation == 'disabled' else 401)
    assert denied.json()['success'] is False


def test_admin_encrypted_issuance_stays_bound_to_target_and_keeps_audit(client):
    seed_targets(client)
    file = issue(client, admin=True)
    token = exchange(client, login(client, proof(client, file), 'FILE_KEY'))
    identity = client.get('/oauth2/userinfo', headers={'Authorization': 'Bearer ' + token}).json()['result']
    assert identity['username'] == 'operator7' and identity['admin'] is False and not identity['permissions']
    assert client.delete('/oauth2/keys/admin/users/8/' + file['keyId']).status_code == 404
    assert client.delete('/oauth2/keys/admin/users/7/' + file['keyId']).status_code == 200
    assert login(client, proof(client, file), 'FILE_KEY').status_code == 401
