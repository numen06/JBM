# logout - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-06 logout

**前置条件**：TC-AUTH-01 已登录

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| logout | logout | DELETE | /oauth2/logout | 接口 success=true |
| userinfo after logout | userinfo after logout | GET | /oauth2/userinfo | 接口 success=true |

