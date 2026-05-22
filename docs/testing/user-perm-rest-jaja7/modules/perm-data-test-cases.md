# 权限数据（菜单/资源） - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-15 管理员可查权限资源与菜单树

**前置条件**：TC-UP-00 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| authority resources | authority resources | GET | /authority/resources | isList:result |
| authority menu tree | authority menu tree | GET | /authority/menus/tree | isList:result |
| user authorities | user authorities | GET | /authority/users/{userId} | isList:result |

## TC-UP-16 OAuth userinfo 含角色与菜单权限码

**前置条件**：TC-UP-00 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| userinfo permissions | userinfo permissions | GET | /oauth2/userinfo | sizeGte:result.menuPermission:0; sizeGte:result.roles:0 |

