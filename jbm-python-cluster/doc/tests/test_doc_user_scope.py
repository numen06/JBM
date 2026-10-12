"""Resource ownership remains mandatory even when a user has a Doc menu/action."""
from fastapi.testclient import TestClient
from test_doc_compat import doc_config
from jbm_cluster_py.platform.doc.main import create_app
from jbm_cluster_py.common.auth import UserInfoAuthClient
from jbm_cluster_py.common.api_access import authorize_api


def test_file_ownership_is_bound_to_authenticated_tenant_and_user(tmp_path, monkeypatch):
    config = doc_config(tmp_path)
    config.raw['jbm']['api-access']['enabled'] = True
    app = create_app(config)
    identities = {
        'alice': {'userId': 7, 'tenantId': 10},
        'bob': {'userId': 8, 'tenantId': 20},
        'alice-moved': {'userId': 7, 'tenantId': 20},
        'root': {'userId': 1, 'tenantId': 0, 'admin': True},
    }

    async def fixture_identity(self, request, service):
        identity = {**identities[request.headers.get('X-Test-Account', 'root')],
                    'permissions': ['MENU_doc_files', 'ACTION_doc_files_add', 'ACTION_doc_files_edit', 'ACTION_doc_files_delete']}
        authorize_api(service, request.method, request.url.path, identity)
        return identity
    monkeypatch.setattr(UserInfoAuthClient, 'authenticate_request', fixture_identity)

    with TestClient(app) as client:
        alice = {'X-Test-Account': 'alice'}
        path = client.post('/upload?group=fixture', files={'file': ('private.txt', b'private-fixture', 'text/plain')}, headers=alice).json()['result']
        for prefix in ['/get/', '/download/']:
            assert client.get(prefix + path, headers=alice).content == b'private-fixture'
            for other in ['bob', 'alice-moved']:
                denied = client.get(prefix + path, headers={'X-Test-Account': other})
                assert denied.status_code == 403 and b'private-fixture' not in denied.content
        assert client.get('/remove/' + path, headers={'X-Test-Account': 'bob'}).status_code == 403
        assert client.get('/get/' + path, headers=alice).content == b'private-fixture'
        for route in ['/baseDoc/pageList', '/baseDoc/model', '/baseDoc/save', '/baseDoc/deleteByPaths', '/baseDocGroup/createTempGroup', '/baseDocGroup/pageList', '/baseDocToken/model']:
            assert client.post(route, json={'paths': [path]}, headers=alice).status_code == 403
        assert client.get('/baseDoc/storageStatus', headers=alice).status_code == 403
        assert client.get('/getViewUrl', params={'fileUrl': path}, headers=alice).status_code == 403
        assert client.post('/baseDoc/pageList', json={}).status_code == 200
        assert client.get('/remove/' + path, headers=alice).status_code == 200
