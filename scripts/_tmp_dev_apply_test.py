#!/usr/bin/env python3
import json
import sys
import urllib.request

sys.path.insert(0, "scripts")
from jbm_cluster_client import login_password

opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
tok = login_password("uapi_dbg001", "Test@api_dbg001")
body = json.dumps({"userType": "dev"})
print("body bytes:", body)
req = urllib.request.Request(
    "http://127.0.0.1:7777/developer/apply",
    data=body.encode("utf-8"),
    method="POST",
    headers={
        "Authorization": f"Bearer {tok}",
        "tenantId": "0",
        "Content-Type": "application/json",
    },
)
try:
    with opener.open(req, timeout=15) as r:
        print("OK", r.status, r.read().decode()[:300])
except urllib.error.HTTPError as e:
    print("ERR", e.code, e.read().decode()[:400])
