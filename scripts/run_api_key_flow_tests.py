#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""外部系统 API 对接全流程 REST 测试（TC1–TC12，走注册/审批/管理端，不直插库）。"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

try:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding
    from cryptography.hazmat.backends import default_backend
except ImportError:
    print("请先安装: pip install cryptography", file=sys.stderr)
    sys.exit(2)

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs/testing/api-key-flow-jaja7"
DOCS.mkdir(parents=True, exist_ok=True)

if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))

from jbm_cluster_client import (  # noqa: E402
    DEFAULT_AUTH,
    DEFAULT_GATEWAY,
    DEFAULT_CENTER,
    wait_services,
    login_password,
    gateway_api,
    unwrap,
)

GW = DEFAULT_GATEWAY
AUTH = DEFAULT_AUTH
CENTER = DEFAULT_CENTER
SEED_CLIENT_ID = "jbmSeedDevAppKey00000001"
SEED_CLIENT_SECRET = "jbmSeedDevSecret0000000001"
ADMIN_USER = "admin"
ADMIN_PASS = os.environ.get("ADMIN_PASSWORD", "Admin@123")

# 与 user_perm / auth REST 套件一致的 OAuth 客户端
os.environ.setdefault("OAUTH_CLIENT_ID", SEED_CLIENT_ID)
os.environ.setdefault("OAUTH_CLIENT_SECRET", SEED_CLIENT_SECRET)

_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


class StepError(RuntimeError):
    pass


def log(msg: str) -> None:
    print(msg, flush=True)


def jb(raw: str):
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def jpath(obj, path: str):
    cur = obj
    for seg in path.split("."):
        if cur is None:
            return None
        if seg.isdigit():
            cur = cur[int(seg)] if isinstance(cur, list) and int(seg) < len(cur) else None
        elif isinstance(cur, dict):
            cur = cur.get(seg)
        else:
            return None
    return cur


def http(method, url, headers=None, body=None, form=False, query=None, timeout=30):
    h = dict(headers or {})
    h.setdefault("tenantId", "0")
    data = None
    if form and body:
        data = body.encode("utf-8")
        h.setdefault("Content-Type", "application/x-www-form-urlencoded")
    elif body is not None and not form:
        data = body.encode("utf-8") if isinstance(body, str) else body
        h.setdefault("Content-Type", "application/json")
    if query:
        qs = urllib.parse.urlencode(query)
        url = url + ("&" if "?" in url else "?") + qs
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with _OPENER.open(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return resp.status, raw, None
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace") if e.fp else ""
        return e.code, raw, str(e)


def assert_success(name, status, raw, allow_http=(200,)):
    body = jb(raw)
    if status not in allow_http:
        raise StepError(f"{name}: HTTP {status} {raw[:300]}")
    if body is not None and body.get("success") is False:
        raise StepError(f"{name}: {body.get('message') or raw[:300]}")
    return body


def oauth_password(username, password):
    form = (
        f"grant_type=password&client_id={urllib.parse.quote(SEED_CLIENT_ID)}"
        f"&client_secret={urllib.parse.quote(SEED_CLIENT_SECRET)}"
        f"&username={urllib.parse.quote(username)}&password={urllib.parse.quote(password)}&scope=all&vcode=9999"
    )
    st, raw, _ = http("POST", f"{AUTH}/oauth2/token", body=form, form=True)
    body = assert_success("oauth password", st, raw)
    token = jpath(body, "result.access_token") or jpath(body, "access_token")
    if not token:
        raise StepError(f"oauth password: no token {raw[:200]}")
    return token


def oauth_client_credentials(client_id, client_secret):
    """第三方 API Key 获取 Token（POST /oauth2/client_token + grant_type=client_credentials）。"""
    form = (
        f"grant_type=client_credentials"
        f"&client_id={urllib.parse.quote(client_id)}"
        f"&client_secret={urllib.parse.quote(client_secret)}&scope=all"
    )
    st, raw, _ = http("POST", f"{GW}/oauth2/client_token", body=form, form=True)
    body = assert_success("client_token", st, raw)
    result = jpath(body, "result") or body
    token = None
    if isinstance(result, dict):
        token = result.get("client_token") or result.get("access_token")
    if not token:
        token = jpath(body, "result.access_token") or jpath(body, "result.client_token")
    if not token:
        raise StepError(f"client_token: no token {raw[:200]}")
    return token


def bearer(token):
    return {"Authorization": f"Bearer {token}"}


def sort_query(qs: str) -> str:
    """与 ApiSecurityUtils.sortQueryString 一致：按 & 分段后字典序拼接。"""
    if not qs:
        return ""
    parts = [p for p in qs.split("&") if p]
    parts.sort()
    return "&".join(parts)


def build_sign_content(method, path, query, body, timestamp, app_id):
    """与 ApiSecurityUtils.buildSignContent 对齐（含空 body 时的额外换行）。"""
    content = "\n".join([method.upper() if method else "", path or "", sort_query(query or "")])
    if body:
        raw = body.encode("utf-8") if isinstance(body, str) else body
        md5_hex = hashlib.md5(raw).hexdigest()
        content += "\n" + base64.b64encode(md5_hex.encode("ascii")).decode("ascii")
    else:
        # Java: query 行后无条件 append('\n')，空 body 时多一行空行
        content += "\n"
    content += f"\n{timestamp}\n{app_id or ''}"
    return content


def rsa_sign(content: str, private_key_b64: str) -> str:
    key_bytes = base64.b64decode(private_key_b64)
    try:
        private_key = serialization.load_der_private_key(key_bytes, password=None, backend=default_backend())
    except Exception:
        private_key = serialization.load_pem_private_key(key_bytes, password=None, backend=default_backend())
    sig = private_key.sign(content.encode("utf-8"), padding.PKCS1v15(), hashes.SHA256())
    return base64.b64encode(sig).decode("ascii")


def authority_ids_for_paths(admin_token, paths: list[str]) -> list:
    """base_api.apiCode -> base_authority.authority (API_{code}) -> authorityId"""
    st, raw, _ = http(
        "GET",
        f"{GW}/api",
        headers=bearer(admin_token),
        query={"serviceId": "jbm-cluster-platform-center"},
    )
    apis = jpath(assert_success("center apis", st, raw), "result") or []
    want = set(paths)
    codes = {a.get("apiCode") for a in apis if a.get("path") in want and a.get("apiCode")}
    if not codes:
        return []
    st2, raw2, _ = http("GET", f"{GW}/authority/catalog", headers=bearer(admin_token), query={"type": "2"})
    catalog = jpath(assert_success("authority catalog", st2, raw2), "result") or []
    ids = []
    for row in catalog:
        auth = row.get("authority") or ""
        if auth.startswith("API_") and auth[4:] in codes:
            aid = row.get("authorityId")
            if aid:
                ids.append(aid)
    return ids


# 全流程固定使用的可读/不可读 API（便于 TC10/TC12）
TEST_GRANT_PATHS = ["/baseDic/getDicMap"]
TEST_DENY_PATH = "/user/list"


def internal_headers():
    return {"X-Internal-Service": "jbm-cluster-platform-gateway"}


def apikey_has_authority(key_id, api_id) -> bool:
    st, raw, _ = http(
        "GET",
        f"{CENTER}/internal/gateway/apikey/{key_id}/check",
        headers=internal_headers(),
        query={"apiId": str(api_id)},
    )
    body = jb(raw)
    return st == 200 and body and body.get("result") is True


def is_read_api(api: dict) -> bool:
    path = (api.get("path") or "").lower()
    mn = (api.get("methodName") or "").lower()
    bad = ("delete", "save", "add", "update", "grant", "export", "batch", "remove", "create", "mock", "close")
    if any(b in path for b in bad):
        return False
    return not any(mn.startswith(b) for b in bad)


def resolve_granted_paths(key_id, admin_token):
    """Grant 后解析可签名调用的 Gateway 路径（优先可读 GET 类 API）。"""
    st, raw, _ = http(
        "GET",
        f"{GW}/api",
        headers=bearer(admin_token),
        query={"serviceId": "jbm-cluster-platform-center"},
    )
    apis = jpath(assert_success("center apis", st, raw), "result") or []
    granted: list[dict] = []
    denied: list[str] = []
    for api in apis:
        path = api.get("path")
        api_id = api.get("apiId")
        if not path or not api_id:
            continue
        if apikey_has_authority(key_id, api_id):
            granted.append(api)
        elif is_read_api(api):
            denied.append(path)
    granted_path = None
    for api in granted:
        if is_read_api(api):
            granted_path = api["path"]
            break
    if not granted_path and granted:
        granted_path = granted[0]["path"]
    denied_path = denied[0] if denied else None
    return granted_path, denied_path


def signed_get(url, api_key, private_key_b64, token=None):
    parsed = urllib.parse.urlparse(url)
    path = parsed.path or "/"
    query = parsed.query
    ts = str(int(time.time() * 1000))
    content = build_sign_content("GET", path, query, "", ts, api_key)
    sig = rsa_sign(content, private_key_b64)
    headers = {}
    if token:
        headers.update(bearer(token))
    headers.update({
        "X-App-Id": api_key,
        "X-Timestamp": ts,
        "X-Signature": sig,
    })
    return http("GET", url, headers=headers)


def run_flow(suffix: str):
    results = []
    ctx = {"suffix": suffix}

    def step(tid, name, fn):
        log(f"\n=== {tid} {name} ===")
        try:
            fn()
            results.append((tid, name, "PASS", ""))
            log(f"PASS {tid}")
        except Exception as e:
            results.append((tid, name, "FAIL", str(e)))
            log(f"FAIL {tid}: {e}")
            write_report(results, datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
            raise StepError(str(e)) from e

    # TC1 注册
    def tc1():
        u = f"uapi_{suffix}"
        p = f"Test@api_{suffix}"
        st, raw, _ = http(
            "POST", f"{GW}/user/registrations",
            query={"userName": u, "password": p, "confirmPassword": p, "nickName": f"API_{suffix}"},
        )
        assert_success("register", st, raw, allow_http=(200, 201))
        ctx["devUser"], ctx["devPass"] = u, p
        token = oauth_password(u, p)
        ctx["devToken"] = token
        st2, raw2, _ = http(
            "GET", f"{AUTH}/oauth2/userinfo",
            headers={**bearer(token), "tenantId": "0"},
            query={"access_token": token},
        )
        body = assert_success("userinfo", st2, raw2)
        uid = jpath(body, "result.userId")
        if not uid:
            raise StepError(f"userId not found: {raw2[:300]}")
        ctx["devUserId"] = uid

    # TC2 申请开发者
    def tc2():
        token = oauth_password(ctx["devUser"], ctx["devPass"])
        ctx["devToken"] = token
        st, raw, _ = http("POST", f"{GW}/developer/apply", headers=bearer(token),
                          body=json.dumps({"userType": "dev"}))
        assert_success("developer apply", st, raw)

    # TC3 管理员审批
    def tc3():
        admin_token = oauth_password(ADMIN_USER, ADMIN_PASS)
        ctx["adminToken"] = admin_token
        st, raw, _ = http("PUT", f"{GW}/developer/{ctx['devUserId']}/approve", headers=bearer(admin_token))
        assert_success("approve developer", st, raw)
        st2, raw2, _ = http("GET", f"{GW}/developer/{ctx['devUserId']}", headers=bearer(admin_token))
        body = assert_success("developer detail", st2, raw2)
        status = jpath(body, "result.status")
        if str(status) != "1":
            raise StepError(f"developer status expected 1, got {status}")

    # 管理员给开发者分配 API 权限（否则 grantable 为空）
    def tc3b_grant_dev_api():
        admin_token = ctx["adminToken"]
        api_ids = authority_ids_for_paths(admin_token, TEST_GRANT_PATHS + [TEST_DENY_PATH])
        if not api_ids:
            st, raw, _ = http("GET", f"{GW}/authority/catalog", headers=bearer(admin_token), query={"type": "2"})
            body = assert_success("api catalog", st, raw)
            apis = jpath(body, "result") or []
            api_ids = [a.get("authorityId") for a in apis if a.get("authorityId")][:5]
        if not api_ids:
            log("WARN: 无 API 权限元数据，跳过用户 API 授权")
            return
        payload = json.dumps({"authorityIds": api_ids})
        st2, raw2, _ = http("PUT", f"{GW}/authority/users/{ctx['devUserId']}", headers=bearer(admin_token), body=payload)
        assert_success("grant user api perms", st2, raw2)
        ctx["sampleAuthorityIds"] = api_ids

    # TC4 创建业务应用
    def tc4():
        token = oauth_password(ctx["devUser"], ctx["devPass"])
        ctx["devToken"] = token
        payload = json.dumps({"appName": f"手机端_{suffix}", "appType": "app", "appCode": f"mobile_{suffix}"})
        st, raw, _ = http("POST", f"{GW}/app", headers=bearer(token), body=payload)
        body = assert_success("create app", st, raw)
        app_id = jpath(body, "result") or jpath(body, "result.appId")
        if isinstance(app_id, dict):
            app_id = app_id.get("appId")
        if not app_id:
            st2, raw2, _ = http("GET", f"{GW}/app", headers=bearer(token))
            b2 = assert_success("list apps", st2, raw2)
            rows = jpath(b2, "result.contents") or jpath(b2, "result") or []
            app_id = rows[0].get("appId") if rows else None
        if not app_id:
            raise StepError("appId missing")
        ctx["bizAppId"] = app_id

    # TC6 个人 API Key
    def tc6():
        token = ctx["devToken"]
        payload = json.dumps({"keyName": f"客户甲_{suffix}", "clientName": "测试公司A"})
        st, raw, _ = http("POST", f"{GW}/apikey", headers=bearer(token), body=payload)
        body = assert_success("create personal apikey", st, raw)
        row = jpath(body, "result") or {}
        if not row.get("apiKey") or not row.get("secretKey"):
            raise StepError(f"apikey create missing secrets: {raw[:300]}")
        ctx["personalKeyId"] = row.get("keyId")
        ctx["personalApiKey"] = row.get("apiKey")
        ctx["personalSecret"] = row.get("secretKey")
        ctx["personalPrivateKey"] = row.get("privateKey")

    # TC7 业务应用下 API Key
    def tc7():
        token = ctx["devToken"]
        payload = json.dumps({
            "keyName": f"客户乙_{suffix}",
            "clientName": "测试公司B",
            "bizAppId": ctx["bizAppId"],
        })
        st, raw, _ = http("POST", f"{GW}/apikey", headers=bearer(token), body=payload)
        body = assert_success("create app apikey", st, raw)
        row = jpath(body, "result") or {}
        ctx["appKeyId"] = row.get("keyId")
        ctx["thirdApiKey"] = row.get("apiKey")
        ctx["thirdSecret"] = row.get("secretKey")
        ctx["thirdPrivateKey"] = row.get("privateKey")

    # TC8 授权（仅授权 1 个 API，便于 TC10/TC12 对比）
    def tc8():
        token = ctx["devToken"]
        st, raw, _ = http("GET", f"{GW}/authority/apis/grantable", headers=bearer(token))
        body = assert_success("grantable apis", st, raw)
        grantable = jpath(body, "result") or []
        if not grantable:
            raise StepError("grantable apis empty — 请先给开发者分配 API 权限")
        first_id = grantable[0].get("authorityId")
        ctx["grantedAuthorityIds"] = [first_id]
        ctx["adminToken"] = oauth_password(ADMIN_USER, ADMIN_PASS)
        grant_ids = authority_ids_for_paths(ctx["adminToken"], TEST_GRANT_PATHS)
        if not grant_ids:
            grant_ids = [g.get("authorityId") for g in grantable if g.get("authorityId")][:5]
        if not grant_ids:
            raise StepError("grantable apis empty — 请先给开发者分配 API 权限")
        payload = json.dumps({"authorityIds": grant_ids})
        st2, raw2, _ = http(
            "PUT", f"{GW}/apikey/{ctx['appKeyId']}/authority",
            headers=bearer(token), body=payload,
        )
        assert_success("grant apikey authority", st2, raw2)
        ctx["adminToken"] = oauth_password(ADMIN_USER, ADMIN_PASS)
        granted_path, denied_path = resolve_granted_paths(ctx["appKeyId"], ctx["adminToken"])
        if not granted_path:
            raise StepError(f"授权后未找到可调用路径，期望含 {TEST_GRANT_PATHS}")
        ctx["grantedApiPath"] = granted_path
        ctx["deniedApiPath"] = denied_path or TEST_DENY_PATH

    # TC9 client_token
    def tc9():
        token = oauth_client_credentials(ctx["thirdApiKey"], ctx["thirdSecret"])
        ctx["thirdAccessToken"] = token

    # TC10 第三方调用已授权 API
    def tc10():
        token = ctx["thirdAccessToken"]
        api_key = ctx["thirdApiKey"]
        priv = ctx.get("thirdPrivateKey")
        if not priv:
            st, raw, _ = http("GET", f"{GW}/apikey/{ctx['appKeyId']}", headers=bearer(ctx["devToken"]))
            body = assert_success("apikey detail", st, raw)
            priv = jpath(body, "result.privateKey")
        if not priv:
            raise StepError("privateKey missing for signing")
        path = ctx.get("grantedApiPath", "/current/user")
        if not path.startswith("/"):
            path = "/" + path
        target = f"{GW}{path}"
        st, raw, _ = signed_get(target, api_key, priv, token=token)
        if st not in (200,):
            raise StepError(f"signed API call failed HTTP {st}: {raw[:300]}")
        body = jb(raw)
        if body and body.get("success") is False:
            raise StepError(f"signed API business fail: {body.get('message')}")

    # TC12 越权拒绝
    def tc12():
        token = ctx["thirdAccessToken"]
        api_key = ctx["thirdApiKey"]
        priv = ctx.get("thirdPrivateKey")
        path = ctx.get("deniedApiPath", "/user")
        if not path.startswith("/"):
            path = "/" + path
        st, raw, _ = signed_get(f"{GW}{path}", api_key, priv)
        if st in (200,) and jb(raw) and jb(raw).get("success") is True:
            raise StepError("expected unauthorized/forbidden but got success")
        if st not in (400, 401, 403, 455, 500):
            log(f"INFO TC12 HTTP {st} body={raw[:200]}")

    step("TC1", "用户注册", tc1)
    step("TC2", "申请开发者", tc2)
    step("TC3", "管理员审批", tc3)
    step("TC3b", "管理员分配 API 权限", tc3b_grant_dev_api)
    step("TC4", "创建业务应用", tc4)
    step("TC6", "创建个人 API Key", tc6)
    step("TC7", "创建应用 API Key", tc7)
    step("TC8", "API Key 授权", tc8)
    step("TC9", "第三方 client_token", tc9)
    step("TC10", "签名调用已授权 API", tc10)
    step("TC12", "越权拒绝", tc12)
    return results


def write_report(results, run_time):
    path = DOCS / "report.md"
    lines = [
        f"# API Key 全流程测试报告",
        "",
        f"- 时间: {run_time}",
        f"- Gateway: {GW}",
        "",
        "| 用例 | 步骤 | 结果 | 备注 |",
        "|------|------|------|------|",
    ]
    for tid, name, status, msg in results:
        lines.append(f"| {tid} | {name} | {status} | {msg[:120]} |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--suffix", default=str(int(time.time() * 1000))[-9:])
    args = ap.parse_args()
    run_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    log(f"API Key flow test suffix={args.suffix}")
    log("集群检测（不启动 Java；请用 jbm_cluster_ops.py status / VS Code 复合启动）")
    ready = wait_services(timeout=90, interval=2.0)
    if not ready.get("gateway") or not ready.get("auth"):
        log(f"FAIL 集群未就绪: {ready}")
        log("  python scripts/jbm_cluster_ops.py status")
        log("  python scripts/jbm_cluster_ops.py wait")
        sys.exit(1)
    from jbm_cluster_client import probe_url, DEFAULT_CENTER

    for i in range(40):
        if probe_url(DEFAULT_CENTER + "/actuator/health"):
            log(f"Center 就绪 ({i})")
            break
        time.sleep(3)
    else:
        log("WARN Center /actuator/health 未就绪，部分用例可能失败")
    st, raw, _ = http("GET", f"{GW}/actuator/health")
    if st != 200:
        log(f"Gateway health failed: {st}")
        sys.exit(1)
    try:
        results = run_flow(args.suffix)
    except StepError as e:
        log(f"\n流程中断: {e}")
        sys.exit(1)
    except Exception as e:
        log(f"\n未预期错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    rp = write_report(results, run_time)
    log(f"\n全部通过，报告: {rp}")
    sys.exit(0)


if __name__ == "__main__":
    main()
