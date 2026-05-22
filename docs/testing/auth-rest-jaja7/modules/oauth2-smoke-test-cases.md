# OAuth2 smoke - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-00 health

**前置条件**：

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| health | health | GET | /actuator/health | 接口 success=true |

## TC-AUTH-03 client_credentials

**前置条件**：

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| client token | client token | POST | /oauth2/token | 接口 success=true |

