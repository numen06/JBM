#!/usr/bin/env python3
import json
import sys
import time

sys.path.insert(0, "scripts")
from jbm_cluster_client import login_password, http_request, parse_json

GW = "http://127.0.0.1:7777"
suffix = str(int(time.time()))[-9:]
u, p = f"uapi_{suffix}", f"Test@api_{suffix}"
http_request(
    "POST",
    f"{GW}/user/registrations?userName={u}&password={p}&confirmPassword={p}&nickName=n",
    {"tenantId": "0"},
)
tok = login_password(u, p)
http_request(
    "POST",
    f"{GW}/developer/apply",
    {"Authorization": f"Bearer {tok}", "tenantId": "0", "Content-Type": "application/json"},
    {"userType": "dev"},
)
admin = login_password("admin", "Admin@123")
st, raw = http_request("GET", f"{AUTH}/oauth2/userinfo?access_token={tok}" if False else f"http://127.0.0.1:5555/oauth2/userinfo?access_token={tok}", {"tenantId": "0"})
uid = parse_json(raw).get("result", {}).get("userId")
http_request("PUT", f"{GW}/developer/{uid}/approve", {"Authorization": f"Bearer {admin}", "tenantId": "0"})
st2, raw2 = http_request(
    "POST",
    f"{GW}/app",
    {"Authorization": f"Bearer {tok}", "tenantId": "0", "Content-Type": "application/json"},
    {"appName": f"mobile_{suffix}", "appType": "app", "appCode": f"mobile_{suffix}"},
)
print("create app", st2, raw2[:800])
