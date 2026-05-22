# 权限 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-01 权限：新用户权限列表与菜单树

**前置条件**：依赖 TC-USER-01 已写入 userId

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 用户权限（允许空列表） | 用户权限（允许空列表） | GET | /authority/users/{userId} | isList:result |
| 菜单权限树 | 菜单权限树 | GET | /authority/menus/tree | isList:result |

