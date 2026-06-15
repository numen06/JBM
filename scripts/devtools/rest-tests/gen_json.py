import json
from pathlib import Path
cfg = {
  "profile": "jaja7",
  "base_url": "http://127.0.0.1:5555",
  "username": "admin",
  "password": "admin123",
  "client_id": "demo",
  "client_secret": "",
  "redirect_uri": "http://127.0.0.1:5555/oauth2/callback",
  "health_path": "/actuator/health",
  "modules": []
}
form = lambda name, method, path, body="", bodyType="", expect="success", **kw: {"name": name, "method": method, "path": path, "body": body, "bodyType": bodyType, "expect": expect, **kw}
cfg["modules"] = [
  {"id": "oauth2-smoke", "title": "smoke", "scenarios": [
    {"id": "TC-AUTH-00", "title": "health", "precondition": "", "steps": [form("health", "GET", "/actuator/health", expect="optional")]},
    {"id": "TC-AUTH-03", "title": "client", "precondition": "", "steps": [form("client", "POST", "/oauth2/token", "grant_type=client_credentials&client_id={client_id}&client_secret={client_secret}&scope=all", "form", "optional", extract={"accessToken": "result.access_token"})]},
  ]},
  {"id": "oauth2-core", "title": "core", "scenarios": [
    {"id": "TC-AUTH-02", "title": "password", "precondition": "", "steps": [form("pwd", "POST", "/oauth2/token", "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}&scope=all", "form", "success", extract={"accessToken": "result.access_token", "refreshToken": "result.refresh_token"}, assert=["notEmpty:result.access_token"])]},
    {"id": "TC-AUTH-05", "title": "userinfo", "precondition": "", "steps": [form("ui", "GET", "/oauth2/userinfo", "access_token={accessToken}", "query", "success", assert=["notNull:result.userId"])]},
    {"id": "TC-AUTH-04", "title": "refresh", "precondition": "", "steps": [form("ref", "POST", "/oauth2/refresh", "grant_type=refresh_token&client_id={client_id}&client_secret={client_secret}&refresh_token={refreshToken}", "form", "success", extract={"accessToken": "result.access_token"})]},
    {"id": "TC-AUTH-12", "title": "renewal", "precondition": "", "steps": [form("ren", "POST", "/oauth2/renewal", "access_token={accessToken}", "form", "success")]},
    {"id": "TC-AUTH-13", "title": "diag", "precondition": "", "steps": [form("diag", "GET", "/token/diagnose/check", "access_token={accessToken}", "query", "optional")]},
    {"id": "TC-AUTH-06", "title": "logout", "precondition": "", "steps": [
      form("out", "DELETE", "/oauth2/logout", expect="optional", headers={"Authorization": "Bearer {accessToken}"}),
      form("ui2", "GET", "/oauth2/userinfo", "access_token={accessToken}", "query", "fail"),
    ]},
  ]},
  {"id": "oauth2-lock", "title": "lock", "scenarios": [
    {"id": "TC-AUTH-07", "title": "lock", "precondition": "", "steps": [
      form("w5", "POST", "/oauth2/token", "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad_{ts}&scope=all", "form", "fail", repeat=5),
      form("l6", "POST", "/oauth2/token", "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}&scope=all", "form", "fail"),
    ]},
    {"id": "TC-AUTH-11", "title": "clear", "precondition": "", "steps": [
      form("w3", "POST", "/oauth2/token", "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password=bad3_{ts}&scope=all", "form", "fail", repeat=3),
      form("ok", "POST", "/oauth2/token", "grant_type=password&client_id={client_id}&client_secret={client_secret}&username={username}&password={password}&scope=all", "form", "success"),
    ]},
  ]},
]
SCRIPTS = Path(__file__).resolve().parents[2]
(SCRIPTS / "auth_rest_modules.json").write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")
print("json ok")