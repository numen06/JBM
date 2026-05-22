# 经网关透传用户 Token - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-30 Gateway 下当前用户与 Auth userinfo 一致

**前置条件**：TC-UP-00

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| gateway current user | gateway current user | GET | /current/user | eq:result.userId:{userId} |
| auth userinfo same user | auth userinfo same user | GET | /oauth2/userinfo | eq:result.userId:{gwUserId} |

