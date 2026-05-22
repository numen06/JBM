# OAuth 登录准备 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-00 password 登录并提取 Token

**前置条件**：Auth 可达；账号 admin 存在

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| password token | password token | POST | /oauth2/token | notEmpty:result.access_token |
| userinfo | userinfo | GET | /oauth2/userinfo | notNull:result.userId; notEmpty:result.roles; notEmpty:result.menuPermission |

