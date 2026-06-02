#!/usr/bin/env python3
"""jaja7 经 Gateway 登录探测：RSA + vcode，直到拿到 access_token。"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

GATEWAY = os.environ.get("JBM_GATEWAY", "http://127.0.0.1:6060").rstrip("/")
CLIENT_ID = os.environ.get("OAUTH_CLIENT_ID", "demo")
CLIENT_SECRET = os.environ.get("OAUTH_CLIENT_SECRET", "demo123")
PASSWORDS = [
    p.strip()
    for p in os.environ.get("LOGIN_PASSWORDS", "Admin@123,admin123").split(",")
    if p.strip()
]
VCODE = os.environ.get("LOGIN_VCODE", "9999")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

_NO_PROXY = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def req(method, url, data=None, headers=None):
    h = dict(headers or {})
    body = None
    if data is not None:
        if isinstance(data, dict):
            body = urllib.parse.urlencode(data).encode("utf-8")
            h.setdefault("Content-Type", "application/x-www-form-urlencoded")
        else:
            body = data
    r = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        with _NO_PROXY.open(r, timeout=30) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")


def parse_json(raw):
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def fetch_public_key():
    for url in (
        f"{GATEWAY}/oauth2/publicKey?app_id={CLIENT_ID}",
        f"{GATEWAY}/captcha/pkey?appKey={CLIENT_ID}",
    ):
        status, raw = req("GET", url)
        jb = parse_json(raw)
        if status == 200 and jb and jb.get("success") and jb.get("result"):
            return jb["result"]
    raise RuntimeError(f"publicKey failed, last: {status} {raw[:300]}")


def rsa_encrypt(plain: str, pub_b64: str) -> str:
    import tempfile

    js = os.path.join(ROOT, "scripts", "rsa_encrypt.js")
    with tempfile.NamedTemporaryFile("w", suffix=".b64", delete=False, encoding="utf-8") as tf:
        tf.write(pub_b64)
        key_path = tf.name
    try:
        proc = subprocess.run(
            ["node", js, plain, key_path],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=30,
        )
    finally:
        os.unlink(key_path)
    if proc.returncode != 0:
        raise RuntimeError(f"RSA encrypt failed: {proc.stderr or proc.stdout}")
    return proc.stdout.strip()


def try_login(username: str, password_plain: str):
    use_plain = os.environ.get("LOGIN_PLAINTEXT", "1") in ("1", "true", "yes")
    if use_plain:
        password_field = password_plain
        headers = {"tenantId": "0"}
    else:
        pub = fetch_public_key()
        password_field = rsa_encrypt(password_plain, pub)
        headers = {"X-Password-Encrypted": "true", "tenantId": "0"}
    form = {
        "grant_type": "password",
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET,
        "username": username,
        "password": password_field,
        "vcode": VCODE,
        "scope": "all",
    }
    status, raw = req("POST", f"{GATEWAY}/oauth2/token", form, headers)
    jb = parse_json(raw)
    msg = (jb or {}).get("message") or raw[:400]
    ok = status in (200, 201) and jb and jb.get("success")
    token = (jb.get("result") or {}).get("access_token") if ok and isinstance(jb.get("result"), dict) else None
    return ok, status, msg, token


def main():
    user = os.environ.get("LOGIN_USER", "admin")
    print(f"Gateway: {GATEWAY}, user: {user}, vcode: {VCODE}")
    st, _ = req("GET", f"{GATEWAY}/actuator/health")
    print(f"health: {st}")
    st2, _ = req("GET", f"{GATEWAY}/captcha/vcode64?width=120&height=40")
    print(f"captcha/vcode64: {st2}")

    for pwd in PASSWORDS:
        print(f"\n--- try password: {pwd!r} ---")
        ok, status, msg, token = try_login(user, pwd)
        print(f"status={status} success={ok} message={msg}")
        if ok and token:
            print(f"LOGIN OK access_token={token[:40]}...")
            return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
