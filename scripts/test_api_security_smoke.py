#!/usr/bin/env python3
"""RSA security smoke: publicKey endpoint and plaintext password rejection."""
import os
import sys
import urllib.parse
import urllib.request

GATEWAY = os.environ.get("JBM_GATEWAY", "http://127.0.0.1:6060")


def get(url):
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return resp.status, resp.read().decode("utf-8", errors="replace")


def post_form(url, data, headers=None):
    body = urllib.parse.urlencode(data).encode("utf-8")
    h = {"Content-Type": "application/x-www-form-urlencoded"}
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, data=body, headers=h, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")


def main():
    ok = True
    base = GATEWAY.rstrip("/")
    status, body = get(base + "/oauth2/publicKey?app_id=demo")
    print("[publicKey] status=%s" % status)
    if status != 200 or "result" not in body:
        print(body[:500])
        ok = False
    status2, body2 = post_form(
        base + "/oauth2/token",
        {
            "grant_type": "password",
            "client_id": "demo",
            "client_secret": "demo123",
            "username": "admin",
            "password": "plaintext-should-fail",
            "scope": "all",
        },
    )
    print("[plaintext login] status=%s" % status2)
    if "password_plaintext_denied" not in body2.lower() and "\u52a0\u5bc6" not in body2:
        print(body2[:500])
        ok = False
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
