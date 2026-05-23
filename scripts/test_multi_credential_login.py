#!/usr/bin/env python3
"""验证同一用户可用用户名、手机号、邮箱三种凭证登录（Gateway OAuth2）。"""
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

_NO_PROXY = urllib.request.build_opener(urllib.request.ProxyHandler({}))

GATEWAY = "http://127.0.0.1:7777"
CLIENT_ID = "demo"
CLIENT_SECRET = "demo123"
PASSWORD = "Admin@123"

CASES = [
    ("demo", ["demo", "13800138000", "demo@jbm.local"]),
    ("viewer", ["viewer", "13900139000", "viewer@jbm.local"]),
    ("admin", ["admin"]),
]


def token_login(username: str) -> dict:
    body = urllib.parse.urlencode(
        {
            "grant_type": "password",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "username": username,
            "password": PASSWORD,
            "scope": "all",
            "loginType": "PASSWORD",
        }
    ).encode("utf-8")
    req = urllib.request.Request(
        f"{GATEWAY}/oauth2/token",
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with _NO_PROXY.open(req, timeout=20) as resp:
        raw = json.loads(resp.read().decode("utf-8"))
    if not raw.get("success") and raw.get("code") != 200:
        raise RuntimeError(raw.get("message") or raw)
    return raw.get("result") or raw


def current_user(access_token: str) -> dict:
    req = urllib.request.Request(
        f"{GATEWAY}/current/user",
        headers={"Authorization": f"Bearer {access_token}", "tenantId": "0"},
        method="GET",
    )
    with _NO_PROXY.open(req, timeout=20) as resp:
        raw = json.loads(resp.read().decode("utf-8"))
    if not raw.get("success") and raw.get("code") != 200:
        raise RuntimeError(raw.get("message") or raw)
    return raw.get("result") or {}


def main() -> int:
    failed = 0
    for label, accounts in CASES:
        print(f"\n=== {label} ===")
        user_ids = set()
        for account in accounts:
            try:
                tok = token_login(account)
                uid = current_user(tok["access_token"]).get("userId")
                user_ids.add(uid)
                print(f"  OK  login as {account!r} -> userId={uid}")
            except (urllib.error.URLError, RuntimeError, KeyError) as e:
                failed += 1
                print(f"  FAIL login as {account!r}: {e}")
        if len(user_ids) > 1:
            failed += 1
            print(f"  FAIL expected same userId, got {user_ids}")
        elif len(user_ids) == 1:
            print(f"  same userId for all credentials: {user_ids.pop()}")
    print(f"\n{'PASSED' if failed == 0 else f'FAILED ({failed})'}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
