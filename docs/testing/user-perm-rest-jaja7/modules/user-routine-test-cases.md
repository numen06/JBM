# 用户常规操作 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-01 当前用户资料

**前置条件**：TC-UP-00 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| current user | current user | GET | /current/user | notNull:result.userId |

## TC-UP-02 当前用户菜单

**前置条件**：TC-UP-00 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| current menus | current menus | GET | /current/user/menus | isList:result |

## TC-UP-03 Center 会话登录

**前置条件**：TC-UP-00 已创建用户

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| POST sessions | POST sessions | POST | /user/sessions | notNull:result.userId |

## TC-UP-04 按用户名查询用户

**前置条件**：已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| list by userName | list by userName | GET | /user | sizeGte:result:1 |

## TC-UP-05 用户统计（需登录态）

**前置条件**：TC-UP-00 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| statistics | statistics | GET | /user/statistics | notNull:result.usersTotal; gte:result.usersTotal:0 |

## TC-UP-06 更新当前用户昵称（可回滚）

**前置条件**：TC-UP-00 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| patch nick | patch nick | PUT | /current/user | 接口 success=true |
| verify nick | verify nick | GET | /current/user | contains:result.nickName:REST_perm_ |

