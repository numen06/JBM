from pathlib import Path
src = Path(r"d:/workspaces/JBM7/scripts/run_center_rest_tests.py")
t = src.read_text(encoding="utf-8")
t = t.replace("center-rest-jaja7", "feign-trust-jaja7")
t = t.replace("center_rest_modules.json", "feign_trust_rest_modules.json")
t = t.replace("Center \u4e1a\u52a1\u573a\u666f REST \u6d4b\u8bd5", "Feign trust REST")
t = t.replace("Center \u4e1a\u52a1\u573a\u666f", "Feign trust")
ins = r'''
def fetch_client_token(cfg, override=""):
    import urllib.parse as up
    if override:
        s = str(override)
        return s if s.startswith("Bearer ") else "Bearer " + s
    base = (cfg.get("auth_base_url") or cfg["base_url"]).rstrip("/")
    url = base + cfg.get("client_token_path", "/oauth2/client_token")
    sep = "&" if "?" in url else "?"
    url = url + sep + up.urlencode({"client_id": cfg.get("client_id", "jbm-cluster-platform-center")})
    h = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
    st, raw, _, _ = http_request("POST", url, h)
    jb = parse_result_body(raw)
    if st not in (200, 201) or not jb:
        return ""
    tok = jb.get("result") or jb.get("access_token") or jb.get("token")
    if isinstance(tok, dict):
        tok = tok.get("access_token") or tok.get("token")
    if not tok:
        return ""
    s = str(tok)
    return s if s.startswith("Bearer ") else "Bearer " + s

def build_feign_headers(step, cfg, ctx):
    h = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
    auth = step.get("auth", "none")
    if auth == "client_only":
        ct = ctx.get("client_token") or ""
        if ct:
            h["Authorization"] = ct
        if step.get("internal_headers"):
            h["X-Internal-Service"] = cfg.get("internal_service", "jbm-cluster-platform-center")
            h["X-Internal-Instance"] = cfg.get("internal_instance", "center:test")
    elif auth == "client_invalid":
        h["Authorization"] = "Bearer invalid-token"
    elif auth == "id_token_only":
        h["Satoken-Id-Token"] = "test-id-only"
    elif auth == "user":
        tok = ctx.get("token") or os.environ.get("CENTER_TOKEN", "")
        if tok:
            h["Authorization"] = tok if str(tok).startswith("Bearer ") else tok
    return h
'''
if "fetch_client_token" not in t:
    t = t.replace("def wait_health(", ins + "\ndef wait_health(")
old = '''    headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
    token = ctx.get("token") or os.environ.get("CENTER_TOKEN", "")
    if token:
        headers["Authorization"] = token if token.startswith("Bearer ") else token'''
new = '''    if step.get("auth"):
        headers = build_feign_headers(step, cfg, ctx)
    else:
        headers = {cfg.get("tenant_header", "tenantId"): cfg.get("tenant_id", "0")}
        token = ctx.get("token") or os.environ.get("CENTER_TOKEN", "")
        if token:
            headers["Authorization"] = token if str(token).startswith("Bearer ") else token'''
t = t.replace(old, new)
t = t.replace(
    'if args.token:\n        ctx["token"] = args.token',
    'if args.token:\n        ctx["token"] = args.token\n    ctx["client_token"] = fetch_client_token(cfg, "")',
)
Path(r"d:/workspaces/JBM7/scripts/run_feign_trust_rest_tests.py").write_text(t, encoding="utf-8")
print("ok")
