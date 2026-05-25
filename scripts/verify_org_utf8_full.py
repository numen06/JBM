#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verify Chinese org save/read via Gateway: save, pageList, tree."""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from jbm_cluster_client import DEFAULT_GATEWAY, gateway_api, login_password, unwrap  # noqa: E402

TARGET_NAME = "测试组织UTF8"
SUFFIX = str(int(time.time()))[-6]
UNIQUE_NAME = f"{TARGET_NAME}_{SUFFIX}"


def find_org(nodes, *, org_id=None, org_name=None):
    for n in nodes or []:
        oid = n.get("id") or n.get("orgId")
        name = n.get("orgName")
        if org_id is not None and (str(oid) == str(org_id) or oid == org_id):
            return n
        if org_name is not None and name == org_name:
            return n
        found = find_org(n.get("children") or [], org_id=org_id, org_name=org_name)
        if found:
            return found
    return None


def main() -> int:
    token = login_password("admin", "Admin@123")
    checks = []

    _, jb, _ = gateway_api("POST", "/baseOrg/tree", token, {})
    tree_before = unwrap(jb) or []
    default_org = find_org(tree_before, org_id=1)
    checks.append(
        {
            "name": "default_org_tree",
            "orgName": default_org.get("orgName") if default_org else None,
            "ok": bool(default_org and default_org.get("orgName") == "默认组织"),
        }
    )

    _, jb_save, _ = gateway_api(
        "POST",
        "/baseOrg/save",
        token,
        {"baseOrg": {"orgName": UNIQUE_NAME, "parentId": 1, "status": 1, "sort": 0}},
    )
    saved = unwrap(jb_save) or {}
    new_id = saved.get("id") or saved.get("orgId")
    checks.append(
        {
            "name": "save_response",
            "orgId": new_id,
            "orgName": saved.get("orgName"),
            "ok": saved.get("orgName") == UNIQUE_NAME,
        }
    )
    if not checks[-1]["ok"]:
        print(json.dumps({"status": "failed", "checks": checks}, ensure_ascii=False, indent=2))
        return 1

    _, jb_page, _ = gateway_api(
        "POST",
        "/baseOrg/pageList",
        token,
        {
            "baseOrg": {"orgName": UNIQUE_NAME},
            "pageForm": {"currPage": 1, "pageSize": 20},
        },
    )
    page = unwrap(jb_page) or {}
    rows = page.get("contents") or []
    page_row = next(
        (r for r in rows if (r.get("id") or r.get("orgId")) == new_id or r.get("orgName") == UNIQUE_NAME),
        None,
    )
    checks.append(
        {
            "name": "pageList",
            "orgName": page_row.get("orgName") if page_row else None,
            "ok": bool(page_row and page_row.get("orgName") == UNIQUE_NAME),
        }
    )

    _, jb_tree, _ = gateway_api("POST", "/baseOrg/tree", token, {})
    tree_after = unwrap(jb_tree) or []
    tree_row = find_org(tree_after, org_id=new_id)
    checks.append(
        {
            "name": "tree_persist",
            "orgName": tree_row.get("orgName") if tree_row else None,
            "ok": bool(tree_row and tree_row.get("orgName") == UNIQUE_NAME),
        }
    )

    # cleanup test org
    if new_id:
        gateway_api("POST", "/baseOrg/delete", token, {"baseOrg": {"id": new_id}})

    failed = [c for c in checks if not c.get("ok")]
    result = {
        "status": "passed" if not failed else "failed",
        "gateway": DEFAULT_GATEWAY,
        "targetName": UNIQUE_NAME,
        "checks": checks,
    }
    out = ROOT / ".cursor" / "e2e-org-utf8-result.json"
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if failed:
        return 1
    print("PASS", out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
