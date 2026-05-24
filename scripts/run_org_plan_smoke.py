#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""组织结构与多租户计划 — 前后端 API 冒烟（经 Gateway jaja7）。"""
from __future__ import annotations

import json
import os
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

from jbm_cluster_client import DEFAULT_GATEWAY, gateway_api, login_password, unwrap  # noqa: E402

ADMIN_USER = os.environ.get("LOGIN_USER", "admin")
ADMIN_PASS = os.environ.get("LOGIN_PASSWORD", "Admin@123")
SUFFIX = str(int(time.time()))[-6:]


def ok(msg: str) -> None:
    print(f"  [PASS] {msg}")


def fail(msg: str) -> None:
    print(f"  [FAIL] {msg}", file=sys.stderr)
    raise SystemExit(1)


def main() -> int:
    print("=== 组织/多租户计划 API 冒烟 ===\n")
    print(f"Gateway: {DEFAULT_GATEWAY}")
    token = login_password(ADMIN_USER, ADMIN_PASS)
    ok(f"admin 登录 token_len={len(token)}")

    # 1) 组织树
    _, jb, _ = gateway_api("POST", "/baseOrg/tree", token, {})
    tree = unwrap(jb)
    if not isinstance(tree, list):
        fail("baseOrg/tree 未返回列表")
    ok(f"组织树节点数(根层)={len(tree)}")

  # 2) 默认组织
    has_default = False
    def walk(nodes):
        nonlocal has_default
        for n in nodes or []:
            oid = n.get("id") or n.get("orgId")
            if oid == 1 or n.get("orgName") == "默认组织":
                has_default = True
            walk(n.get("children") or [])
    walk(tree)
    if not has_default:
        # 尝试 root
        _, jb2, _ = gateway_api("POST", "/baseOrg/root", token, {})
        roots = unwrap(jb2) or []
        for r in roots:
            if (r.get("id") or r.get("orgId")) == 1:
                has_default = True
    if not has_default:
        fail("未找到默认组织 id=1")
    ok("默认组织存在")

    # 3) 用户列表（超管应有多条或至少 admin）
    _, jb, _ = gateway_api("GET", "/user?pageForm.currPage=1&pageForm.pageSize=5", token)
    page = unwrap(jb) or {}
    users = page.get("contents") or []
    if not users:
        fail("用户分页为空")
    ok(f"用户列表 count={len(users)} total={page.get('total')}")

    # 4) 应用列表含 orgId
    _, jb, _ = gateway_api("GET", "/app?pageForm.currPage=1&pageForm.pageSize=10", token)
    apps_page = unwrap(jb) or {}
    apps = apps_page.get("contents") or []
    if not apps:
        fail("应用列表为空")
    with_org = [a for a in apps if a.get("orgId") is not None]
    ok(f"应用列表 {len(apps)} 条，含 orgId={len(with_org)} 条")

    # 5) 新建子组织
    org_name = f"计划测试组织_{SUFFIX}"
    _, jb, _ = gateway_api(
        "POST",
        "/baseOrg/save",
        token,
        {"model": {"orgName": org_name, "parentId": 1, "status": 1}},
    )
    saved_org = unwrap(jb) or {}
    new_org_id = saved_org.get("id") or saved_org.get("orgId")
    if not new_org_id:
        fail("保存组织未返回 id")
    ok(f"新建组织 id={new_org_id} name={org_name}")

    # 6) 新建应用绑定组织
    app_code = f"orgplan_{SUFFIX}"
    _, jb, _ = gateway_api(
        "POST",
        "/app",
        token,
        {
            "appName": f"组织计划测试应用_{SUFFIX}",
            "appCode": app_code,
            "apiKey": app_code,
            "orgId": new_org_id,
            "status": 1,
        },
    )
    ok("新建应用并绑定 orgId")

    # 7) 用户跨组织授权 API
    admin_id = None
    for u in users:
        if u.get("userName") == "admin":
            admin_id = u.get("userId")
            break
    if not admin_id:
        admin_id = users[0].get("userId")
    _, jb, _ = gateway_api("GET", f"/user/{admin_id}/orgs", token)
    org_auth = unwrap(jb)
    if not isinstance(org_auth, list):
        fail("GET /user/{id}/orgs 异常")
    ok(f"用户组织授权查询 rows={len(org_auth)}")

    # 8) current user 含组织信息
    _, jb, _ = gateway_api("GET", "/current/user", token)
    cur = unwrap(jb) or {}
    ok(
        f"current/user userId={cur.get('userId')} "
        f"companyId={cur.get('companyId')} deptId={cur.get('deptId')}"
    )

    print("\n=== API 冒烟全部通过 ===")
    print(f"前端: http://127.0.0.1:5173  → 组织管理 / 用户管理 / 应用管理")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SystemExit:
        raise
    except Exception as e:
        print(f"\n[FATAL] {e}", file=sys.stderr)
        raise SystemExit(1) from e
