"""Group capabilities cannot enumerate or delete documents in another group."""
import pytest
from fastapi.testclient import TestClient
from test_doc_compat import doc_config
from jbm_cluster_py.platform.doc.main import create_app


@pytest.mark.parametrize('selector', ['ids', 'paths'])
def test_group_token_cannot_delete_foreign_file_or_partially_delete_batch(tmp_path, selector):
    with TestClient(create_app(doc_config(tmp_path))) as client:
        def group(path):
            return client.post('/baseDocGroup/createTempGroup', json={'groupPath': path}).json()['result']
        first, second = group('team'), group('team-foreign')
        def upload(token):
            return client.post('/baseDocGroup/uploadByToken', headers={'Doc-Token-Key': token}, files={'file': ('check.txt', b'security-test-fixture', 'text/plain')}).json()['result']
        own, foreign = upload(first['tokenKey']), upload(second['tokenKey'])
        items = client.post('/baseDocGroup/findGroupItemByToken', headers={'Doc-Token-Key': first['tokenKey']}, json={}).json()['result']
        assert [row['docPath'] for row in items] == [own]
        docs = client.post('/baseDoc/pageList', json={}).json()['result']['contents']
        by_path = {row['docPath']: row['docId'] for row in docs}
        values = [own, foreign] if selector == 'paths' else [by_path[own], by_path[foreign]]
        denied = client.post('/baseDocGroup/removeGroupItemByToken', headers={'Doc-Token-Key': first['tokenKey']}, json={selector: values})
        assert denied.status_code == 403
        assert all(client.get('/get/' + path).content == b'security-test-fixture' for path in [own, foreign])
        allowed = client.post('/baseDocGroup/removeGroupItemByToken', headers={'Doc-Token-Key': first['tokenKey']}, json={selector: values[:1]})
        assert allowed.status_code == 200 and allowed.json()['success']
        assert client.get('/get/' + foreign).content == b'security-test-fixture'


@pytest.mark.parametrize('expiration', ['invalid', '2020-01-01T00:00:00+00:00'])
def test_invalid_or_expired_group_capability_is_rejected(tmp_path, expiration):
    with TestClient(create_app(doc_config(tmp_path))) as client:
        group = client.post('/baseDocGroup/createTempGroup', json={}).json()['result']
        client.post('/baseDocToken/save', json={'baseDocToken': {'tokenKey': group['tokenKey'], 'expirationTime': expiration}})
        response = client.post('/baseDocGroup/findGroupItemByToken', headers={'Doc-Token-Key': group['tokenKey']}, json={})
        assert response.status_code in {400, 401, 403}
