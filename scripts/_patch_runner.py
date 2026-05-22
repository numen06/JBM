# -*- coding: utf-8 -*-
from pathlib import Path

src = Path(__file__).resolve().parent / "run_center_rest_tests.py"
dst = Path(__file__).resolve().parent / "run_auth_rest_tests.py"
t = src.read_text(encoding="utf-8")
for a, b in [
    ("Center \u4e1a\u52a1\u573a\u666f REST \u6d4b\u8bd5", "Auth OAuth2 REST \u6d4b\u8bd5"),
    ("center-rest-jaja7", "auth-rest-jaja7"),
    ("center_rest_modules.json", "auth_rest_modules.json"),
    ("jbm-cluster-platform-center", "jbm-cluster-platform-auth"),
    ("def start_center", "def start_auth"),
    ("start_center(", "start_auth("),
    ("Center \u4e1a\u52a1\u573a\u666f", "Auth OAuth2"),
    ("center \u672a\u5c31\u7eea", "auth \u672a\u5c31\u7eea"),
    ("run_center_rest_tests", "run_auth_rest_tests"),
    ("CENTER_MODULE", "AUTH_MODULE"),
    ("CENTER_TOKEN", "AUTH_TOKEN"),
]:
    t = t.replace(a, b)

ins = """        elif op == "contains":
            path, expected = rest.rsplit(":", 1)
            actual = json_path(jb, path) if path != "message" else (jb or {}).get("message", "")
            expected = expand(expected, ctx)
            if expected not in str(actual or ""):
                failures.append(f"{rule} 实际={actual!r}")
        elif op == "notEmpty":
            v = json_path(jb, rest)
            if v is None or v == "" or v == [] or v == {}:
                failures.append(f"{rule} 为空")
"""
if 'op == "contains"' not in t:
    t = t.replace('        elif op == "notNull":', ins + '        elif op == "notNull":')

if "--smoke" not in t:
    t = t.replace(
        '    ap.add_argument("--token", default="")\n    args = ap.parse_args()',
        '    ap.add_argument("--token", default="")\n    ap.add_argument("--smoke", action="store_true")\n    args = ap.parse_args()',
    )
    t = t.replace(
        '    for mod in cfg["modules"]:',
        '    modules = [m for m in cfg["modules"] if (not args.smoke or m.get("id") == "oauth2-smoke")]\n    for mod in modules:',
    )

# auth: optional step headers + accessToken in ctx
t = t.replace(
    '    asserts = step.get("assert") or []\n\n    miss = _missing_ctx',
    '    asserts = step.get("assert") or []\n    step_headers = step.get("headers") or {}\n\n    miss = _missing_ctx',
)
t = t.replace(
    '    headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}\n    token = ctx.get("token") or os.environ.get("AUTH_TOKEN", "")',
    '    headers = {k: expand(v, ctx) for k, v in step_headers.items()}\n    token = ctx.get("token") or ctx.get("accessToken") or os.environ.get("AUTH_TOKEN", "")',
)
t = t.replace(
    '        headers["Authorization"] = token if token.startswith("Bearer ") else token',
    '        headers["Authorization"] = token if str(token).startswith("Bearer ") else f"Bearer {token}"',
)

old_start = '''def start_auth(profile):
    cmd = (
        f"mvn -pl {AUTH_MODULE} -am spring-boot:run "
        f"-Dspring-boot.run.profiles={profile} -DskipTests=true"
    )
    print("[start]", cmd, flush=True)
    return subprocess.Popen(cmd, cwd=str(ROOT), shell=True)'''
new_start = '''def start_auth(profile):
    auth_dir = ROOT / "jbm-cluster" / "jbm-cluster-platform" / "jbm-cluster-platform-auth"
    cmd = f'mvn spring-boot:run "-Dspring-boot.run.profiles={profile}" -DskipTests=true'
    print("[start]", cmd, "cwd=", auth_dir, flush=True)
    return subprocess.Popen(cmd, cwd=str(auth_dir), shell=True)'''
if old_start in t:
    t = t.replace(old_start, new_start)
else:
    new_start2 = '''def start_auth(profile):
    cluster = ROOT / "jbm-cluster"
    cmd = (
        f"mvn -pl {AUTH_MODULE} -am spring-boot:run "
        f'"-Dspring-boot.run.profiles={profile}" '
        f'"-Dspring-boot.run.main-class=com.jbm.cluster.auth.JbmAuthApplication" '
        f"-DskipTests=true"
    )
    print("[start]", cmd, "cwd=", cluster, flush=True)
    return subprocess.Popen(cmd, cwd=str(cluster), shell=True)'''
    t = t.replace(
        'def start_auth(profile):\n    auth_dir = ROOT / "jbm-cluster"',
        'def start_auth(profile):\n    cluster = ROOT / "jbm-cluster"\n    auth_dir = ROOT / "jbm-cluster"',
    )
    if 'spring-boot.run.main-class' not in t:
        t = t.replace(
            '''def start_auth(profile):
    auth_dir = ROOT / "jbm-cluster" / "jbm-cluster-platform" / "jbm-cluster-platform-auth"
    cmd = f'mvn spring-boot:run "-Dspring-boot.run.profiles={profile}" -DskipTests=true'
    print("[start]", cmd, "cwd=", auth_dir, flush=True)
    return subprocess.Popen(cmd, cwd=str(auth_dir), shell=True)''',
            new_start2,
        )
oauth_ctx = '''    ctx = {
        "ts": ts,
        "usuffix": ts[-9:],
        "username": cfg.get("username", "admin"),
        "password": cfg.get("password", "admin123"),
        "client_id": cfg.get("client_id", "demo"),
        "client_secret": cfg.get("client_secret", ""),
        "redirect_uri": cfg.get("redirect_uri", ""),
    }'''
t = t.replace(
    '''    ctx = {
        "ts": ts,
        "usuffix": ts[-9:],
        "username": cfg.get("username", "admin"),
        "password": cfg.get("password", "admin123"),
    }''',
    oauth_ctx,
)
dst.write_text(t, encoding="utf-8")
print("runner ok", dst.stat().st_size)

import json

def step(name, method, path, body="", bodyType="", expect="success", **kw):
    s = {"name": name, "method": method, "path": path, "body": body, "bodyType": bodyType, "expect": expect}
    if "assrt" in kw:
        kw["assert"] = kw.pop("assrt")
    s.update(kw)
    return s

cfg = {
    "profile": "jaja7",
    "base_url": "http://127.0.0.1:5555",
    "username": "admin",
    "password": "admin123",
    "client_id": "demo",
    "client_secret": "demo123",
    "redirect_uri": "http://127.0.0.1:5555/oauth2/callback",
    "health_path": "/actuator/health",
    "modules": [
        {"id": "oauth2-smoke", "title": "OAuth2 smoke", "scenarios": [
            {"id": "TC-AUTH-00", "title": "health", "precondition": "", "steps": [
                step("health", "GET", "/actuator/health", expect="optional")
            ]},
            {"id": "TC-AUTH-03", "title": "client_credentials", "precondition": "", "steps": [
                step("client token", "POST", "/oauth2/token",
                     "grant_type=client_credentials&client_id={client_id}&client_secret={client_secret}&scope=all",
                     "form", "optional", extract={"accessToken": "result.access_token"})
            ]},
        ]},
        {"id": "oauth2-core", "title": "OAuth2 core", "scenarios": [
            {"id": "TC-AUTH-02", "title": "password grant", "precondition": "", "steps": [
                step("password token", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}&scope=all",
                     "form", "success",
                     extract={"accessToken": "result.access_token", "refreshToken": "result.refresh_token"},
                     assrt=["notEmpty:result.access_token"])
            ]},
            {"id": "TC-AUTH-05", "title": "userinfo", "precondition": "", "steps": [
                step("userinfo", "GET", "/oauth2/userinfo", "access_token={accessToken}", "query", "success",
                     assrt=["notNull:result.userId", "notEmpty:result.roles", "notEmpty:result.menuPermission"])
            ]},
            {"id": "TC-AUTH-04", "title": "refresh", "precondition": "", "steps": [
                step("refresh", "POST", "/oauth2/refresh",
                     "grant_type=refresh_token&client_id={client_id}&client_secret={client_secret}&refresh_token={refreshToken}",
                     "form", "success", extract={"accessToken": "result.access_token"})
            ]},
            {"id": "TC-AUTH-12", "title": "renewal", "precondition": "", "steps": [
                step("renewal", "POST", "/oauth2/renewal", "access_token={accessToken}", "form", "success")
            ]},
            {"id": "TC-AUTH-13", "title": "diagnose", "precondition": "", "steps": [
                step("diagnose", "GET", "/token/diagnose/check", "access_token={accessToken}", "query", "optional")
            ]},
            {"id": "TC-AUTH-06", "title": "logout", "precondition": "", "steps": [
                step("logout", "DELETE", "/oauth2/logout", expect="optional",
                     headers={"Authorization": "Bearer {accessToken}"}),
                step("userinfo after logout", "GET", "/oauth2/userinfo", "access_token={accessToken}", "query", "fail"),
            ]},
        ]},
        {"id": "oauth2-lock", "title": "login lock", "scenarios": [
            {"id": "TC-AUTH-07", "title": "lock after 5 fails", "precondition": "", "steps": [
                step("wrong 1", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad1_{ts}&scope=all", "form", "fail"),
                step("wrong 2", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad2_{ts}&scope=all", "form", "fail"),
                step("wrong 3", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad3_{ts}&scope=all", "form", "fail"),
                step("wrong 4", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad4_{ts}&scope=all", "form", "fail"),
                step("wrong 5", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad5_{ts}&scope=all", "form", "fail"),
                step("locked", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}&scope=all",
                     "form", "fail", assrt=["contains:message:超限"]),
            ]},
            {"id": "TC-AUTH-11", "title": "clear error count", "precondition": "", "steps": [
                step("wrong 1", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=x1_{ts}&scope=all", "form", "fail"),
                step("wrong 2", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=x2_{ts}&scope=all", "form", "fail"),
                step("wrong 3", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=x3_{ts}&scope=all", "form", "fail"),
                step("correct login", "POST", "/oauth2/token",
                     "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}&scope=all",
                     "form", "success"),
            ]},
        ]},
    ],
}
json_path = Path(__file__).resolve().parent / "auth_rest_modules.json"
json_path.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")
print("json ok", json_path.stat().st_size)
