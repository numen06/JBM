# -*- coding: utf-8 -*-
from pathlib import Path
src = Path(r"D:\workspaces\JBM7\scripts\run_center_rest_tests.py")
dst = Path(r"D:\workspaces\JBM7\scripts\run_feign_trust_rest_tests.py")
t = src.read_text(encoding="utf-8")
for a, b in [
    ("Center 业务场景 REST 测试", "Feign 互信 REST 测试"),
    ("center-rest-jaja7", "feign-trust-jaja7"),
    ("center_rest_modules.json", "feign_trust_rest_modules.json"),
    ("Center 业务场景", "Feign 互信"),
    ("center 未就绪", "服务未就绪"),
    ("run_center_rest_tests.py", "run_feign_trust_rest_tests.py"),
]:
    t = t.replace(a, b)
fetch_fn = """

def fetch_client_token(cfg, override=\"\"):
    import urllib.parse
    if override:
        return override if str(override).startswith(\"Bearer \") else \"Bearer \" + str(override)
    base = (cfg.get(\"auth_base_url\") or cfg[\"base_url\"]).rstrip(\"/\")
    path = cfg.get(\"client_token_path\", \"/token/diagnose/client-token\")
    cid = cfg.get(\"client_id\", \"jbm-cluster-platform-center-jbm7\")
    url = base + path + (\"&\" if \"?\" in path else \"?\") + urllib.parse.urlencode({\"clientId\": cid})
    headers = {cfg.get(\"tenant_header\", \"tenantId\"): cfg.get(\"tenant_id\", \"0\")}
    status, raw, _, err = http_request(\"POST\", url, headers)
    jb = parse_result_body(raw)
    if status not in (200, 201) or not jb or not jb.get(\"success\"):
        print(\"[warn] ClientToken failed:\", status, err, (jb or {}).get(\"message\"))
        return \"\"
    result = jb.get(\"result\") or {}
    token = result.get(\"client_token\") if isinstance(result, dict) else result
    if not token:
        return \"\"
    s = str(token)
    return s if s.startswith(\"Bearer \") else \"Bearer \" + s


def trust_simulation_headers(cfg):
    ip = cfg.get(\"simulate_remote_ip\") or \"\"
    if not ip:
        return {}
    return {\"X-Forwarded-For\": ip}


def build_feign_headers(step, cfg, ctx):
    headers = {cfg.get(\"tenant_header\", \"tenantId\"): cfg.get(\"tenant_id\", \"0\")}
    headers.update(trust_simulation_headers(cfg))
    auth = step.get(\"auth\", \"none\")
    if auth == \"client_only\":
        ct = ctx.get(\"client_token\") or \"\"
        if ct:
            headers[\"Authorization\"] = ct
        if step.get(\"internal_headers\"):
            headers[\"X-Internal-Service\"] = cfg.get(\"internal_service\", \"jbm-cluster-platform-center-jbm7\")
            headers[\"X-Internal-Instance\"] = cfg.get(\"internal_instance\", \"center:test\")
    elif auth == \"client_invalid\":
        headers[\"Authorization\"] = \"Bearer invalid-token-for-feign-trust-test\"
        if step.get(\"internal_headers\"):
            headers[\"X-Internal-Service\"] = cfg.get(\"internal_service\", \"jbm-cluster-platform-center-jbm7\")
            headers[\"X-Internal-Instance\"] = cfg.get(\"internal_instance\", \"center:test\")
    elif auth == \"id_token_only\":
        headers[\"Satoken-Id-Token\"] = \"feign-trust-test-id-token-only\"
    elif auth == \"user\":
        token = ctx.get(\"token\") or os.environ.get(\"CENTER_TOKEN\", \"\")
        if token:
            headers[\"Authorization\"] = token if str(token).startswith(\"Bearer \") else token
    return headers
"""
if "def fetch_client_token" not in t:
    t = t.replace("def wait_health(", fetch_fn + "\ndef wait_health(")
old_hdr = '''    headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
    token = ctx.get("token") or os.environ.get("CENTER_TOKEN", "")
    if token:
        headers["Authorization"] = token if token.startswith("Bearer ") else token'''
new_hdr = '''    if step.get("auth"):
        headers = build_feign_headers(step, cfg, ctx)
    else:
        headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
        headers.update(trust_simulation_headers(cfg))
        token = ctx.get("token") or os.environ.get("CENTER_TOKEN", "")
        if token:
            headers["Authorization"] = token if str(token).startswith("Bearer ") else token'''
t = t.replace(old_hdr, new_hdr)
t = t.replace('    ap.add_argument("--token", default="")\n    args = ap.parse_args()', '    ap.add_argument("--token", default="")\n    ap.add_argument("--client-token", default="")\n    args = ap.parse_args()')
t = t.replace('    if args.token:\n        ctx["token"] = args.token\n', '    if args.token:\n        ctx["token"] = args.token\n    ctx["client_token"] = fetch_client_token(cfg, args.client_token)\n')
t = t.replace('description="Center 业务场景 REST 测试"', 'description="Feign 互信 REST 测试"')
t = t.replace("def start_center(", "def _start_center_unused(")
t = t.replace("if not service_ok and args.start:", "if False and args.start:")
t = t.replace("timeout=8)", "timeout=args.wait)", 1)
t = t.replace('ap.add_argument("--wait", type=int, default=180)', 'ap.add_argument("--wait", type=int, default=8)')
dst.write_text(t, encoding="utf-8")
print("written", dst)