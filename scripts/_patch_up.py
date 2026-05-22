import shutil
from pathlib import Path

src = Path(r"d:/workspaces/JBM7/scripts/run_center_rest_tests.py")
dst = Path(r"d:/workspaces/JBM7/scripts/run_user_perm_rest_tests.py")
shutil.copyfile(src, dst)
t = dst.read_text(encoding="utf-8")
repls = [
    ("Center 业务场景 REST 测试", "用户常规操作与权限控制 REST 测试"),
    ("docs/testing/center-rest-jaja7", "docs/testing/user-perm-rest-jaja7"),
    ("scripts/center_rest_modules.json", "scripts/user_perm_rest_modules.json"),
    ('CTX_TOKENS = ("userId", "roleId", "formCode", "customFormId", "roleCode", "testUserName")',
     'CTX_TOKENS = ("userId", "roleId", "accessToken", "refreshToken", "gwUserId", "username", "client_id", "client_secret")'),
    ('description="Center 业务场景 REST 测试"', 'description="用户与权限 REST 测试"'),
    ("# Center 业务场景测试汇总", "# 用户与权限 REST 测试汇总"),
    ("run_center_rest_tests.py", "run_user_perm_rest_tests.py"),
    ('"center 未就绪"', '"服务未就绪"'),
]
for a, b in repls:
    t = t.replace(a, b)

helpers = r'''
def resolve_base(cfg, step):
    if (step.get("service") or "gateway").lower() == "auth":
        return (cfg.get("auth_base_url") or cfg["base_url"]).rstrip("/")
    return cfg["base_url"].rstrip("/")


def build_auth_headers(step, cfg, ctx):
    headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
    auth_mode = step.get("auth", "bearer")
    if auth_mode == "none":
        return headers
    if auth_mode == "invalid":
        headers["Authorization"] = "Bearer invalid-user-perm-test-token"
        return headers
    token = ctx.get("token") or ctx.get("accessToken") or os.environ.get("CENTER_TOKEN", "")
    if token:
        headers["Authorization"] = token if str(token).startswith("Bearer ") else f"Bearer {token}"
    return headers


def sync_token_after_extract(ctx):
    tok = ctx.get("accessToken") or ""
    if tok and not ctx.get("token"):
        ctx["token"] = tok if str(tok).startswith("Bearer ") else f"Bearer {tok}"


def fetch_password_access_token(cfg, ctx):
    base = (cfg.get("auth_base_url") or cfg["base_url"]).rstrip("/")
    url = base + "/oauth2/token"
    body = (
        "grant_type=password&client_id={client_id}&client_secret={client_secret}"
        "&username={username}&password={password}&scope=all"
    )
    body = expand(body, {**ctx, "client_id": cfg.get("client_id", "demo"),
                         "client_secret": cfg.get("client_secret", "demo123"),
                         "username": cfg.get("username", "admin"),
                         "password": cfg.get("password", "admin123")})
    headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
    status, raw, _, err = http_request("POST", url, headers, body, "form")
    jb = parse_result_body(raw)
    if status not in (200, 201) or not jb or not jb.get("success"):
        print("[warn] OAuth password failed:", status, err, (jb or {}).get("message"))
        return
    result = jb.get("result") or {}
    if isinstance(result, dict):
        tok = result.get("access_token") or ""
        if tok:
            ctx["accessToken"] = tok
            ctx["token"] = tok if str(tok).startswith("Bearer ") else f"Bearer {tok}"


def wait_health_dual(cfg, timeout=20):
    base = cfg["base_url"].rstrip("/")
    auth_base = (cfg.get("auth_base_url") or base).rstrip("/")
    health = cfg.get("health_path", "/actuator/health")
    th, tid = cfg.get("tenant_header", "tenantId"), cfg.get("tenant_id", "0")
    headers = {th: tid}
    deadline = time.time() + timeout
    gw_ok = False
    while time.time() < deadline:
        for url in (base + health, base + "/role/all"):
            status, raw, _, _ = http_request("GET", url, headers)
            if status == 200:
                jb = parse_result_body(raw)
                if jb is None or jb.get("success") is True or "UP" in raw:
                    gw_ok = True
        st, raw, _, _ = http_request("GET", auth_base + "/actuator/health", headers)
        auth_ok = st == 200 and ("UP" in raw or (parse_result_body(raw) or {}).get("success"))
        if gw_ok and auth_ok:
            return True
        time.sleep(2)
    return gw_ok

'''
marker = "def wait_health(base_url, health_path"
if marker not in t:
    raise SystemExit("marker missing")
t = t.replace(marker, helpers + marker, 1)
t = t.replace(
    '    headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}\n    token = ctx.get("token") or os.environ.get("CENTER_TOKEN", "")\n    if token:\n        headers["Authorization"] = token if token.startswith("Bearer ") else token',
    '    headers = build_auth_headers(step, cfg, ctx)',
)
t = t.replace(
    '    base = cfg["base_url"].rstrip("/")\n    url = expand(path, ctx)\n    if not url.startswith("http"):\n        url = base + url',
    '    base = resolve_base(cfg, step)\n    url = expand(path, ctx)\n    if not url.startswith("http"):\n        url = base + url',
)
t = t.replace(
    "            apply_extract(extract, ctx, jb, path_fc)\n            if asserts:",
    "            apply_extract(extract, ctx, jb, path_fc)\n            sync_token_after_extract(ctx)\n            if asserts:",
)
t = t.replace(
    '    ap.add_argument("--wait", type=int, default=180)',
    '    ap.add_argument("--auth-url", default="")\n    ap.add_argument("--wait", type=int, default=20)',
)
t = t.replace(
    "    if args.base_url:\n        cfg[\"base_url\"] = args.base_url\n    cfg[\"profile\"] = args.profile",
    "    if args.base_url:\n        cfg[\"base_url\"] = args.base_url\n    if args.auth_url:\n        cfg[\"auth_base_url\"] = args.auth_url\n    cfg[\"profile\"] = args.profile",
)
t = t.replace(
    '        "password": cfg.get("password", "admin123"),\n    }\n    if args.token:\n        ctx["token"] = args.token\n\n    th, tid = cfg.get("tenant_header", "tenantId"), cfg.get("tenant_id", "0")\n    service_ok = wait_health(cfg["base_url"], cfg["health_path"], th, tid, timeout=8)',
    '        "password": cfg.get("password", "admin123"),\n        "client_id": cfg.get("client_id", "demo"),\n        "client_secret": cfg.get("client_secret", "demo123"),\n    }\n    if args.token:\n        ctx["token"] = args.token\n        ctx["accessToken"] = args.token.replace("Bearer ", "").strip()\n    elif not ctx.get("token"):\n        fetch_password_access_token(cfg, ctx)\n\n    service_ok = wait_health_dual(cfg, timeout=args.wait if not args.start else max(args.wait, 8))',
)
t = t.replace(
    '        f"- 地址: {cfg[\'base_url\']}",',
    "        f\"- Gateway: {cfg['base_url']}\",\n        f\"- Auth: {cfg.get('auth_base_url', cfg['base_url'])}\",",
)
t = t.replace(
    '        elif op == "isNull":\n            v = jb.get(rest) if rest == "success" and jb else json_path(jb, rest)\n            if v is not None:\n                failures.append(f"{rule} 实际={v!r}")',
    '        elif op == "isNull":\n            v = jb.get(rest) if rest == "success" and jb else json_path(jb, rest)\n            if v is not None:\n                failures.append(f"{rule} 实际={v!r}")\n        elif op == "contains":\n            path, expected = rest.rsplit(":", 1)\n            actual = json_path(jb, path) if path != "message" else (jb or {}).get("message", "")\n            expected = expand(expected, ctx)\n            if expected not in str(actual or ""):\n                failures.append(f"{rule} 实际={actual!r}")',
)

t = t.replace(
    "            cur = cur[int(seg)] if isinstance(cur, list) else None",
    "            if isinstance(cur, list):\n                i = int(seg)\n                cur = cur[i] if 0 <= i < len(cur) else None\n            else:\n                cur = None",
)
dst.write_text(t, encoding="utf-8")
print("patched", dst.read_bytes()[:4])