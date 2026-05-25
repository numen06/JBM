#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify org id=1 Chinese name save/read via Gateway."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from jbm_cluster_client import gateway_api, login_password, unwrap  # noqa: E402

TARGET_NAME = "组织1"


def find_org_id1(nodes):
    for n in nodes or []:
        oid = n.get("id") or n.get("orgId")
        if str(oid) == "1" or oid == 1:
            return n
        found = find_org_id1(n.get("children") or [])
        if found:
            return found
    return None


def main() -> int:
    token = login_password("admin", "Admin@123")
    _, jb, _ = gateway_api("POST", "/baseOrg/tree", token, {})
    tree = unwrap(jb) or []
    before = find_org_id1(tree)
    print("before:", json.dumps(before, ensure_ascii=False) if before else None)

    _, jb2, _ = gateway_api(
        "POST",
        "/baseOrg/save",
        token,
        {"baseOrg": {"id": 1, "orgName": TARGET_NAME, "status": 1}},
    )
    saved = unwrap(jb2) or {}
    print("saved:", json.dumps(saved, ensure_ascii=False))
    if saved.get("orgName") != TARGET_NAME:
        print(f"FAIL save orgName={saved.get('orgName')!r}", file=sys.stderr)
        return 1

    _, jb3, _ = gateway_api("POST", "/baseOrg/tree", token, {})
    after = find_org_id1(unwrap(jb3) or [])
    print("after:", json.dumps(after, ensure_ascii=False) if after else None)
    if not after or after.get("orgName") != TARGET_NAME:
        print(f"FAIL tree orgName={after.get('orgName') if after else None!r}", file=sys.stderr)
        return 1

    result = {
        "status": "passed",
        "beforeOrgName": before.get("orgName") if before else None,
        "afterOrgName": after.get("orgName"),
    }
    out = ROOT / ".cursor" / "e2e-org-save-cn-result.json"
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS", out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
