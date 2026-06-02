#!/usr/bin/env python3
"""
超管造数 + 双角色双用户 RBAC 对比测试（Gateway API 断言）。

流程：
  1. admin 登录
  2. setup_test_users_via_admin 逻辑（operator/demo、editor/viewer）
  3. 对 admin / demo / viewer 断言菜单、ACTION_*、多凭证、用户列表权限
"""
from __future__ import annotations

import json
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

GATEWAY = "http://127.0.0.1:6060"
CLIENT_ID = "demo"
CLIENT_SECRET = "demo123"
ADMIN = "admin"
PWD = "Admin@123"

_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))
ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "docs/testing/auth-rest-jaja7/rbac-compare-test-report.md"


@dataclass
class Row:
    area: str
    user: str
    check: str
    expected: str
    actual: str
    passed: bool
    note: str = ""


@dataclass
class Report:
    rows: list[Row] = field(default_factory=list)
    issues: list[str] = field(default_factory=list)

    def add(self, area, user, check, expected, actual, passed, note=""):
        self.rows.append(Row(area, user, check, expected, str(actual), passed, note))
        if not passed:
            self.issues.append(f"[{user}] {check}: expected {expected}, got {actual}. {note}")

    def write(self):
        REPORT.parent.mkdir(parents=True, exist_ok=True)
        passed = sum(1 for r in self.rows if r.passed)
        total = len(self.rows)
        lines = [
            "# RBAC 对比测试报告",
            "",
            f"- 时间: {datetime.now().isoformat(timespec='seconds')}",
            f"- Gateway: `{GATEWAY}`",
            f"- 结果: **{passed}/{total}** 通过",
            "",
        ]
        if self.issues:
            lines.append("## 逻辑/代码疑点")
            for i in self.issues:
                lines.append(f"- {i}")
            lines.append("")
        lines.append("## 断言明细")
        lines.append("")
        lines.append("| 领域 | 用户 | 检查项 | 期望 | 实际 | 结果 |")
        lines.append("|------|------|--------|------|------|------|")
        for r in self.rows:
            mark = "PASS" if r.passed else "**FAIL**"
            lines.append(
                f"| {r.area} | {r.user} | {r.check} | {r.expected} | {r.actual[:80]} | {mark} |"
            )
        REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"\n报告已写入: {REPORT}")


def api(method, path, token=None, body=None, form=False):
    url = GATEWAY + path if path.startswith("/") else path
    headers = {"tenantId": "0"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None
    if body is not None:
        if form:
            data = urllib.parse.urlencode(body).encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with _OPENER.open(req, timeout=45) as resp:
        return json.loads(resp.read().decode("utf-8"))


def unwrap(jb):
    if jb.get("success") is False and jb.get("code") not in (200, None):
        raise RuntimeError(jb.get("message") or json.dumps(jb, ensure_ascii=False)[:300])
    return jb.get("result")


def login(user, password=PWD):
    jb = api(
        "POST",
        "/oauth2/token",
        body={
            "grant_type": "password",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "username": user,
            "password": password,
            "scope": "all",
            "loginType": "PASSWORD",
        },
        form=True,
    )
    return unwrap(jb)["access_token"]


def auth_set(user_data) -> set[str]:
    auths = user_data.get("authorities") or []
    return {a.get("authority") for a in auths if a.get("authority")}


def menu_paths(token) -> set[str]:
    menus = unwrap(api("GET", "/current/user/menus", token))
    paths = set()
    for m in menus or []:
        p = m.get("path")
        if p and p != "/":
            paths.add(p if p.startswith("/") else f"/{p}")
    return paths


def has_auth(codes: set[str], key: str) -> bool:
    return key in codes


def run_setup():
    script = ROOT / "scripts/setup_test_users_via_admin.py"
    print("== 超管造数 ==")
    proc = subprocess.run(
        [sys.executable, str(script), "--password", PWD],
        cwd=str(ROOT),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    out = (proc.stdout or "") + (proc.stderr or "")
    if out.strip():
        sys.stdout.buffer.write(out.encode("utf-8", errors="replace"))
    if proc.returncode != 0:
        raise RuntimeError("setup_test_users_via_admin 失败")
    return out


def main():
    rep = Report()
    print("== admin 登录 ==")
    try:
        admin_tok = login(ADMIN)
    except Exception as e:
        rep.add("环境", ADMIN, "Gateway 登录", "200", e, False)
        rep.write()
        return 1

    # 用户列表应可访问
    try:
        page = unwrap(api("GET", "/user?pageForm.currPage=1&pageForm.pageSize=5", admin_tok))
        rep.add("API", ADMIN, "GET /user 分页", "success", f"total={page.get('total')}", True)
    except Exception as e:
        rep.add("API", ADMIN, "GET /user 分页", "success", e, False, "用户列表接口异常")

    run_setup()

    expectations = {
        "admin": {
            "menus_must": {"/dashboard", "/system/users", "/system/roles"},
            "menus_min": 8,
            "actions_must": {"ACTION_users_add", "ACTION_dict_delete"},
            "actions_deny": set(),
            "credentials": ["admin"],
        },
        "demo": {
            "menus_must": {"/dashboard", "/system/dicts"},
            "menus_min": 2,
            "actions_must": {"ACTION_dict_view", "ACTION_dict_add"},
            "actions_deny": {"ACTION_dict_delete", "ACTION_users_add"},
            "credentials": ["demo", "13800138000", "demo@jbm.local"],
        },
        "viewer": {
            "menus_must": {"/system/users"},
            "menus_min": 1,
            "actions_must": {"ACTION_users_view", "ACTION_users_edit"},
            "actions_deny": {"ACTION_users_add", "ACTION_users_delete"},
            "credentials": ["viewer", "13900139000", "viewer@jbm.local"],
        },
    }

    for username, exp in expectations.items():
        print(f"\n== 用户 {username} ==")
        uids = set()
        tok = None
        for cred in exp["credentials"]:
            try:
                tok = login(cred)
                u = unwrap(api("GET", "/current/user", tok))
                uids.add(str(u.get("userId")))
                rep.add("多凭证", username, f"登录 {cred}", "成功", u.get("userId"), True)
            except Exception as e:
                rep.add("多凭证", username, f"登录 {cred}", "成功", e, False)

        rep.add(
            "多凭证",
            username,
            "userId 唯一",
            "1",
            len(uids),
            len(uids) == 1 and len(exp["credentials"]) > 0,
            f"ids={uids}",
        )

        if not tok:
            continue

        codes = auth_set(unwrap(api("GET", "/current/user", tok)))
        paths = menu_paths(tok)

        for p in exp["menus_must"]:
            rep.add(
                "菜单",
                username,
                f"含 {p}",
                "true",
                p in paths,
                p in paths,
            )
        rep.add(
            "菜单",
            username,
            "菜单数量",
            f">={exp['menus_min']}",
            len(paths),
            len(paths) >= exp["menus_min"],
            ", ".join(sorted(paths)[:12]),
        )

        for a in exp["actions_must"]:
            rep.add("按钮", username, f"含 {a}", "true", a in codes, a in codes)
        for a in exp["actions_deny"]:
            rep.add(
                "按钮",
                username,
                f"不含 {a}",
                "false",
                a in codes,
                a not in codes,
            )

        # 交叉：demo 不应有用户管理菜单（若配置了则 FAIL）
        if username == "demo":
            rep.add(
                "逻辑",
                username,
                "无用户管理菜单",
                "false",
                "/system/users" in paths,
                "/system/users" not in paths,
            )
        if username == "viewer":
            rep.add(
                "逻辑",
                username,
                "无字典管理菜单",
                "false",
                "/system/dicts" in paths,
                "/system/dicts" not in paths,
            )

    # 角色授权是否写回（造数后重新登录，避免 token 失效）
    try:
        admin_tok = login(ADMIN)
        roles = unwrap(api("GET", "/role/all", admin_tok))
        codes = {r.get("roleCode") for r in roles or []}
        rep.add("造数", ADMIN, "角色 operator/editor", "存在", codes, {"operator", "editor"}.issubset(codes))
    except Exception as e:
        rep.add("造数", ADMIN, "角色列表", "ok", e, False)

    rep.write()
    failed = sum(1 for r in rep.rows if not r.passed)
    print(f"\nSUMMARY: {len(rep.rows) - failed}/{len(rep.rows)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
