# 登录守卫 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-10 未带 Token 访问当前用户应拒绝

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| no token current user | no token current user | GET | /current/user | isFalse:success |

## TC-UP-11 无效 Token 访问当前用户应拒绝

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| bad token | bad token | GET | /current/user | 接口 success=true |

## TC-UP-12 未登录访问当前菜单应拒绝

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| no token menus | no token menus | GET | /current/user/menus | 接口 success=true |

