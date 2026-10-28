"""Password CAPTCHA is enforced after a failed attempt, then reset on success."""

import asyncio

import pytest
from fastapi.testclient import TestClient
from jbm_cluster_py.platform.auth.main import create_app
from test_auth_compat import PKCE_CHALLENGE, auth_config, seed_database


@pytest.mark.parametrize("endpoint", ["/oauth2/doLogin", "/oauth2/token"])
def test_first_password_login_needs_no_captcha_but_retry_cannot_bypass_it(tmp_path, endpoint):
    url = f"sqlite+aiosqlite:///{tmp_path / 'captcha.db'}"
    asyncio.run(seed_database(url))
    with TestClient(create_app(auth_config(url))) as client:
        form = {
            "client_id": "JBM",
            "username": "admin",
            "password": "admin123",
            "grant_type": "password",
            "response_type": "code",
            "redirect_uri": "http://admin.test/login/callback",
            "code_challenge": PKCE_CHALLENGE,
            "code_challenge_method": "S256",
        }
        assert client.post(endpoint, data=form).status_code == 200
        wrong = client.post(endpoint, data={**form, "password": "wrong"})
        assert wrong.status_code in (400, 401)
        assert wrong.json()["captcha_required"] is True
        missing = client.post(endpoint, data=form)
        assert missing.status_code == 400
        assert missing.json()["captcha_required"] is True
        invalid = client.post(endpoint, data={**form, "vcode": "not-a-valid-captcha"})
        assert invalid.status_code == 400
        assert invalid.json()["captcha_required"] is True
        cache = client.app.state.auth_service.cache
        client.portal.call(cache.set_json, "captcha:system:test5", {"code": "TEST5"}, 60)
        assert client.post(endpoint, data={**form, "vcode": "TEST5"}).status_code == 200
        assert client.post(endpoint, data=form).status_code == 200


def test_captcha_requirement_is_shared_by_workers_and_does_not_check_password_without_proof():
    from jbm_cluster_py.platform.auth.service import AuthError
    from test_session_lifecycle import Repository, backends, service

    class PasswordRepository(Repository):
        calls = 0

        async def find_account(self, *args):
            self.calls += 1
            return {**self.account, "password": "correct"}

    async def exercise():
        async with backends() as (cache, shared):
            repo = PasswordRepository()
            first, second = service(cache, repo), service(shared, repo)
            form = {"username": "alice", "password": "incorrect"}
            with pytest.raises(AuthError) as failed:
                await first._authenticate_user_for_client(repo.client, form)
            assert failed.value.captcha_required is True
            calls = repo.calls
            with pytest.raises(AuthError) as challenged:
                await second._authenticate_user_for_client(
                    repo.client, {**form, "password": "correct"}
                )
            assert challenged.value.captcha_required is True
            assert repo.calls == calls

    asyncio.run(exercise())
