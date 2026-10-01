"""Opt-in Chromium test of the shipped TypeScript helper and a virtual authenticator."""

import os
import subprocess
from pathlib import Path
from urllib.parse import urlsplit

import pytest
from test_key_login import client, login  # noqa: F401 (shared fixture)


@pytest.mark.skipif(os.environ.get("JBM_AUTH_BROWSER_TEST") != "1", reason="Opt-in browser test")
def test_browser_passkey_registration_and_login(client):  # noqa: F811 - imported pytest fixture
    from playwright.sync_api import sync_playwright

    admin = Path(__file__).resolve().parents[3] / "jbm-admin-vue"
    compile_result = subprocess.run(
        [
            "node",
            "--input-type=module",
            "-e",
            "import ts from 'typescript'; import fs from 'node:fs'; "
            "console.log(ts.transpileModule(fs.readFileSync('src/lib/keyLogin.ts','utf8'),"
            "{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ES2022}}).outputText)",
        ],
        cwd=admin,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        timeout=30,
    )
    client.app.state.auth_service.key_login.rp_id = "localhost"
    client.app.state.auth_service.key_login.origins = ["http://localhost"]
    token = client.headers["Authorization"][7:]

    def route(request_route):
        request = request_route.request
        path = urlsplit(request.url).path
        if path == "/":
            request_route.fulfill(
                content_type="text/html", body="<html><body>Passkey test</body></html>"
            )
        elif path == "/keyLogin.js":
            request_route.fulfill(content_type="text/javascript", body=compile_result.stdout)
        else:
            response = client.request(
                request.method, path, content=request.post_data_buffer, headers=request.headers
            )
            request_route.fulfill(
                status=response.status_code, content_type="application/json", body=response.content
            )

    with sync_playwright() as p:
        executable = os.environ.get("JBM_AUTH_CHROMIUM_EXECUTABLE")
        browser = p.chromium.launch(
            headless=True, **({"executable_path": executable} if executable else {})
        )
        try:
            page = browser.new_page()
            page.route("http://localhost/**", route)
            page.goto("http://localhost/")
            cdp = page.context.new_cdp_session(page)
            cdp.send("WebAuthn.enable")
            cdp.send(
                "WebAuthn.addVirtualAuthenticator",
                {
                    "options": {
                        "protocol": "ctap2",
                        "transport": "internal",
                        "hasResidentKey": True,
                        "hasUserVerification": True,
                        "isUserVerified": True,
                        "automaticPresenceSimulation": True,
                    }
                },
            )
            proof = page.evaluate(
                """async token => {
                const { createKeyClient } = await import('/keyLogin.js');
                const keys = createKeyClient('', () => token);
                await keys.registerPasskey('Chromium passkey');
                const records = await keys.list();
                if (records.length !== 1) throw new Error('Credential not saved');
                return createKeyClient('', () => '').passkeyProof('JBM');
            }""",
                token,
            )
            import json

            response = login(client, json.loads(proof))
            assert response.status_code == 200, response.text
        finally:
            browser.close()
