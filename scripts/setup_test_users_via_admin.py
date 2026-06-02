#!/usr/bin/env python3
"""
通过超级管理员 API 创建 RBAC 测试数据（非代码种子）。
用于验证菜单/按钮/多凭证，无需反复改 Initializer 或重启 Center。

用法（Gateway 6060 已启动）:
  python scripts/setup_test_users_via_admin.py
  python scripts/jbm_cluster_ops.py setup-rbac
"""
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

GATEWAY = "http://127.0.0.1:6060"
CLIENT_ID = "demo"
CLIENT_SECRET = "demo123"
ADMIN_USER = "admin"

# 与 AdminVueRbacSeedInitializer 一致（菜单 authorityId = menuId）
SEED_MENU_AUTHORITY_ID = {
    "MENU_dashboard": "110",
    "MENU_users": "102",
    "MENU_dicts": "109",
    "MENU_account_logs": "131",
}

# (actionCode, menuCode, actionName)
STANDARD_ACTIONS = [
    ("users_view", "users", "用户-查看"),
    ("users_add", "users", "用户-新增"),
    ("users_edit", "users", "用户-编辑"),
    ("users_delete", "users", "用户-删除"),
    ("dict_view", "dicts", "字典-查看"),
    ("dict_add", "dicts", "字典-新增"),
    ("dict_edit", "dicts", "字典-编辑"),
    ("dict_delete", "dicts", "字典-删除"),
]

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


def build_authority_index(token):
    """authority 字符串 -> authorityId"""
    index = {}

    try:
        catalog = unwrap(request("GET", f"{GATEWAY}/authority/catalog?type=1", token)) or []
        for item in catalog:
            a = item.get("authority")
            aid = item.get("authorityId")
            if a and aid:
                index[a] = str(aid)
    except Exception:
        pass

    if not index:
        try:
            admin = unwrap(request("GET", f"{GATEWAY}/current/user", token)) or {}
            for item in admin.get("authorities") or []:
                a = item.get("authority")
                aid = item.get("authorityId")
                if a and aid:
                    index[a] = str(aid)
        except Exception:
            pass

    try:
        menus = unwrap(request("GET", f"{GATEWAY}/menu/all", token)) or []
        for m in menus:
            code = m.get("menuCode")
            mid = m.get("menuId")
            if code and mid:
                index[f"MENU_{code}"] = str(mid)
    except Exception:
        pass

    index.update(SEED_MENU_AUTHORITY_ID)
    return index


def ensure_standard_actions(token, index):
    """库中无按钮元数据时，由超管通过 /action 补建（与种子定义一致）。"""
    for action_code, menu_code, action_name in STANDARD_ACTIONS:
        key = f"ACTION_{action_code}"
        if key in index:
            continue
        menu_id = index.get(f"MENU_{menu_code}") or SEED_MENU_AUTHORITY_ID.get(f"MENU_{menu_code}")
        if not menu_id:
            continue
        try:
            request(
                "POST",
                f"{GATEWAY}/action",
                token,
                {
                    "actionCode": action_code,
                    "actionName": action_name,
                    "menuId": int(menu_id),
                    "status": 1,
                    "priority": 1,
                },
            )
        except Exception:
            pass
    # 刷新 ACTION_* 索引
    try:
        catalog = unwrap(request("GET", f"{GATEWAY}/authority/catalog?type=1", token)) or []
        for item in catalog:
            a = item.get("authority")
            aid = item.get("authorityId")
            if a and aid and a.startswith("ACTION_"):
                index[a] = str(aid)
    except Exception:
        pass


def aid(index, key):
    v = index.get(key)
    if not v:
        raise RuntimeError(f"未找到权限 {key}，请确认 Center 已写入标准菜单/按钮且超管已授权")
    return v


FALLBACK_ROLE_ID = {"operator": 2, "editor": 3, "super_admin": 1}


def list_roles(token):
    roles = unwrap(request("GET", f"{GATEWAY}/role/all", token)) or []
    if roles:
        return roles
    page = unwrap(
        request("GET", f"{GATEWAY}/role?pageForm.currPage=1&pageForm.pageSize=200", token)
    )
    contents = (page or {}).get("contents") or []
    if contents:
        return contents
    return []


def find_role_id_by_code(token, code):
    for r in list_roles(token):
        if r.get("roleCode") == code:
            return r.get("roleId")
    for rid in FALLBACK_ROLE_ID.values():
        try:
            r = unwrap(request("GET", f"{GATEWAY}/role/{rid}", token))
            if r and r.get("roleCode") == code:
                return r.get("roleId")
        except Exception:
            continue
    return FALLBACK_ROLE_ID.get(code)


def ensure_role(token, code, name, authority_ids):
    role_id = find_role_id_by_code(token, code)
    ids = [x for x in authority_ids if x]
    if role_id is None:
        jb = request(
            "POST",
            f"{GATEWAY}/role",
            token,
            {"roleCode": code, "roleName": name, "status": 1, "remark": "脚本创建"},
        )
        if jb.get("success") is False and jb.get("code") not in (200, None):
            role_id = find_role_id_by_code(token, code)
            if role_id is None:
                raise RuntimeError(jb.get("message") or jb)
        else:
            role_id = unwrap(jb)
    if ids:
        jb2 = request("PUT", f"{GATEWAY}/authority/roles/{role_id}", token, {"authorityIds": ids})
        if jb2.get("success") is False and jb2.get("code") not in (200, None):
            raise RuntimeError(jb2.get("message") or jb2)
    return role_id


def find_user_id(token, user_name):
    try:
        users = unwrap(request("GET", f"{GATEWAY}/user?keyword={urllib.parse.quote(user_name)}", token))
        if isinstance(users, list):
            for u in users:
                if u.get("userName") == user_name:
                    return u["userId"]
    except Exception:
        pass
    for curr in range(1, 8):
        try:
            page = unwrap(
                request(
                    "GET",
                    f"{GATEWAY}/user?pageForm.currPage={curr}&pageForm.pageSize=50",
                    token,
                )
            )
        except Exception:
            break
        for u in (page or {}).get("contents") or []:
            if u.get("userName") == user_name:
                return u["userId"]
        if not (page or {}).get("contents"):
            break
    return None


def ensure_user(token, user_name, nick_name, mobile, email, password, role_id):
    uid = find_user_id(token, user_name)
    if uid is None:
        try:
            request(
                "POST",
                f"{GATEWAY}/user",
                token,
                {
                    "userName": user_name,
                    "nickName": nick_name,
                    "mobile": mobile,
                    "email": email,
                    "password": password,
                    "status": 1,
                    "userType": "normal",
                },
            )
        except urllib.error.HTTPError:
            pass
        uid = find_user_id(token, user_name)
    if uid is None:
        raise RuntimeError(f"未找到用户 {user_name}")
    request(
        "PUT",
        f"{GATEWAY}/user/{uid}",
        token,
        {
            "nickName": nick_name,
            "mobile": mobile,
            "email": email,
            "status": 1,
            "roleIds": [str(role_id)],
            **({"password": password} if password else {}),
        },
    )
    return uid


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--password", default="Admin@123", help="超管/测试用户密码")
    args = parser.parse_args()

    print("1. 超管登录 …")
    token = login(ADMIN_USER, args.password)

    print("2. 解析权限目录 …")
    idx = build_authority_index(token)
    print(f"   已解析 {len(idx)} 条（补建前）")
    print("2b. 补建标准按钮（若缺失）…")
    ensure_standard_actions(token, idx)
    print(f"   已解析 {len(idx)} 条权限标识")

    print("3. 配置角色 operator …")
    op_id = ensure_role(
        token,
        "operator",
        "运营人员",
        [
            aid(idx, "MENU_dashboard"),
            aid(idx, "MENU_dicts"),
            aid(idx, "MENU_account_logs"),
            aid(idx, "ACTION_dict_view"),
            aid(idx, "ACTION_dict_add"),
        ],
    )
    print(f"   roleId={op_id}")

    print("4. 配置角色 editor …")
    ed_id = ensure_role(
        token,
        "editor",
        "业务编辑",
        [
            aid(idx, "MENU_users"),
            aid(idx, "ACTION_users_view"),
            aid(idx, "ACTION_users_edit"),
        ],
    )
    print(f"   roleId={ed_id}")

    pwd = args.password
    print("5. 用户 demo …")
    demo_id = ensure_user(token, "demo", "演示运营", "13800138000", "demo@jbm.local", pwd, op_id)
    print(f"   userId={demo_id}")

    print("6. 用户 viewer …")
    viewer_id = ensure_user(token, "viewer", "只读编辑", "13900139000", "viewer@jbm.local", pwd, ed_id)
    print(f"   userId={viewer_id}")

    # 超管角色合并全部权限项，便于 admin 账号测全量菜单/按钮
    all_ids = list(dict.fromkeys(idx.values()))
    try:
        for r in list_roles(token):
            if r.get("roleCode") in ("super_admin", "root", "administrator") or r.get("roleId") == 1:
                request(
                    "PUT",
                    f"{GATEWAY}/authority/roles/{r['roleId']}",
                    token,
                    {"authorityIds": all_ids},
                )
                print(f"7. 已刷新超管角色权限 ({len(all_ids)} 项)")
                break
    except Exception as e:
        print(f"7. 跳过超管权限刷新: {e}")

    print("\n完成。验证账号（密码同 --password）:")
    print("  demo   : demo | 13800138000 | demo@jbm.local")
    print("  viewer : viewer | 13900139000 | viewer@jbm.local")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, RuntimeError) as e:
        print("失败:", e, file=sys.stderr)
        sys.exit(1)
