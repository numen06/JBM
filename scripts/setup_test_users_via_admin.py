#!/usr/bin/env python3
"""
通过超级管理员 API 创建 RBAC 测试数据（非代码种子）。
用于验证菜单/按钮/多凭证，无需反复改 Initializer 或重启 Center。

用法（Gateway 7777 已启动）:
  python scripts/setup_test_users_via_admin.py
  python scripts/setup_test_users_via_admin.py --password Admin@123
"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

GATEWAY = "http://127.0.0.1:7777"
CLIENT_ID = "demo"
CLIENT_SECRET = "demo123"
ADMIN_USER = "admin"

_OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def request(method, url, token=None, body=None, form=False):
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
    with _OPENER.open(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def unwrap(jb):
    if jb.get("success") is False and jb.get("code") not in (200, None):
        raise RuntimeError(jb.get("message") or jb)
    return jb.get("result")


def login(username, password):
    jb = request(
        "POST",
        f"{GATEWAY}/oauth2/token",
        body={
            "grant_type": "password",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "username": username,
            "password": password,
            "scope": "all",
            "loginType": "PASSWORD",
        },
        form=True,
    )
    return unwrap(jb)["access_token"]


def find_authority_id(catalog, authority_key):
    for item in catalog:
        if item.get("authority") == authority_key:
            return str(item["authorityId"])
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--password", default="Admin@123", help="超管密码")
    args = parser.parse_args()

    print("1. 超管登录 …")
    token = login(ADMIN_USER, args.password)
    h_base = f"{GATEWAY}"

    print("2. 加载权限目录 …")
    catalog = unwrap(request("GET", f"{h_base}/authority/catalog?type=1", token))

    menu_dashboard = find_authority_id(catalog, "MENU_dashboard")
    menu_dicts = find_authority_id(catalog, "MENU_dicts")
    menu_logs = find_authority_id(catalog, "MENU_account_logs")
    menu_users = find_authority_id(catalog, "MENU_users")
    act_dict_view = find_authority_id(catalog, "ACTION_dict_view")
    act_dict_add = find_authority_id(catalog, "ACTION_dict_add")
    act_users_view = find_authority_id(catalog, "ACTION_users_view")
    act_users_edit = find_authority_id(catalog, "ACTION_users_edit")

    def create_role(code, name, authority_ids):
        body = {"roleCode": code, "roleName": name, "status": 1, "remark": "脚本创建"}
        jb = request("POST", f"{h_base}/role", token, body)
        role_id = unwrap(jb)
        ids = [x for x in authority_ids if x]
        if ids:
            request("PUT", f"{h_base}/authority/roles/{role_id}", token, {"authorityIds": ids})
        return role_id

    print("3. 创建角色 operator …")
    op_id = create_role(
        "operator",
        "运营人员",
        [menu_dashboard, menu_dicts, menu_logs, act_dict_view, act_dict_add],
    )
    print(f"   roleId={op_id}")

    print("4. 创建角色 editor …")
    ed_id = create_role(
        "editor",
        "业务编辑",
        [menu_users, act_users_view, act_users_edit],
    )
    print(f"   roleId={ed_id}")

    def create_user(user_name, nick_name, mobile, email, password, role_id):
        body = {
            "userName": user_name,
            "nickName": nick_name,
            "mobile": mobile,
            "email": email,
            "password": password,
            "status": 1,
            "userType": "normal",
        }
        request("POST", f"{h_base}/user", token, body)
        users = unwrap(request("GET", f"{h_base}/user?keyword={user_name}", token))
        if isinstance(users, list) and users:
            uid = users[0]["userId"]
        else:
            page = unwrap(
                request(
                    "GET",
                    f"{h_base}/user?pageForm.currPage=1&pageForm.pageSize=20",
                    token,
                )
            )
            uid = next(
                (u["userId"] for u in page.get("contents", []) if u.get("userName") == user_name),
                None,
            )
        if uid is None:
            raise RuntimeError(f"未找到新建用户 {user_name}")
        request(
            "PUT",
            f"{h_base}/user/{uid}",
            token,
            {
                "nickName": nick_name,
                "mobile": mobile,
                "email": email,
                "status": 1,
                "roleIds": [str(role_id)],
            },
        )
        return uid

    pwd = args.password
    print("5. 创建用户 demo（运营角色 + 手机/邮箱凭证）…")
    demo_id = create_user("demo", "演示运营", "13800138000", "demo@jbm.local", pwd, op_id)
    print(f"   userId={demo_id}")

    print("6. 创建用户 viewer（编辑角色 + 手机/邮箱凭证）…")
    viewer_id = create_user("viewer", "只读编辑", "13900139000", "viewer@jbm.local", pwd, ed_id)
    print(f"   userId={viewer_id}")

    print("\n完成。可用以下账号验证（密码与 --password 相同）:")
    print("  demo   : demo | 13800138000 | demo@jbm.local")
    print("  viewer : viewer | 13900139000 | viewer@jbm.local")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, RuntimeError) as e:
        print("失败:", e, file=sys.stderr)
        sys.exit(1)
