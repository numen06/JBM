# 方法级权限 SaCheckPermission - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-20 在线用户列表（管理员）

**前置条件**：TC-UP-00；admin 用于在线监控踢人场景可选

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| online pageList | online pageList | POST | /online/pageList | notNull:result.total; isList:result.contents |

## TC-UP-21 踢人接口需登录且带权限码

**前置条件**：TC-UP-00；admin 用于在线监控踢人场景可选

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| kickout no auth | kickout no auth | DELETE | /online/kickout/not-a-real-token | 接口 success=true |
| kickout with admin | kickout with admin | DELETE | /online/kickout/{accessToken} | 接口 success=true |

## TC-UP-22 注销在线会话接口权限

**前置条件**：TC-UP-00；admin 用于在线监控踢人场景可选

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| logout online no auth | logout online no auth | DELETE | /online/logout/fake-token-id | 接口 success=true |
| logout online admin | logout online admin | DELETE | /online/logout/{accessToken} | 接口 success=true |

