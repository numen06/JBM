#!/usr/bin/env python3
"""Quick TC9 probe: client_credentials vs client_token."""
import json
import sys
import time
import urllib.parse
import urllib.request

sys.path.insert(0, "scripts")
from jbm_cluster_client import login_password, http_request, parse_json, unwrap

GW = "http://127.0.0.1:7777"
AUTH = "http://127.0.0.1:5555"
admin = login_password("admin", "Admin@123")

# reuse dev from last run or create minimal
suffix = str(int(time.time()))[-9:]
u, p = f"uapi_{suffix}", f"Test@api_{suffix}"
http_request("POST", f"{GW}/user/registrations?userName={u}&password={p}&confirmPassword={p}&nickName=n", {"tenantId": "0"})
dev = login_password(u, p)
st, raw = http_request("GET", f"{AUTH}/oauth2/userinfo?access_token={dev}", {"tenantId": "0"})
uid = parse_json(raw).get("result", {}).get("userId")
http_request("POST", f"{GW}/developer/apply", {"Authorization": f"Bearer {dev}", "tenantId": "0", "Content-Type": "application/json"}, {"userType": "dev"})
http_request("PUT", f"{GW}/developer/{uid}/approve", {"Authorization": f"Bearer {admin}", "tenantId": "0"})
dev = login_password(u, p)
st, raw = http_request("POST", f"{GW}/apikey", {"Authorization": f"Bearer {dev}", "tenantId": "0", "Content-Type": "application/json"}, {"keyName": "t", "clientName": "c"})
row = unwrap(parse_json(raw))
cid, sec = row["apiKey"], row["secretKey"]
print("apiKey", cid)

for path in ("/oauth2/token", "/oauth2/client_token"):
    for gt in ("client_credentials", "client_credential"):
        form = urllib.parse.urlencode({"grant_type": gt, "client_id": cid, "client_secret": sec, "scope": "all"})
        st, body = http_request("POST", f"{AUTH}{path}", {"tenantId": "0", "Content-Type": "application/x-www-form-urlencoded"}, form)
        print(path, gt, st, body[:250])
