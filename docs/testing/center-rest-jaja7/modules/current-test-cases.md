# 当前用户 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-CUR-01 当前用户：需登录 Token

**前置条件**：已设置 Authorization 或 CENTER_TOKEN

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| GET /current/user | GET /current/user | GET | /current/user | notNull:result.userId |

