# Feign test user - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-TRUST-00 register oauth

**前置条件**：up

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| register | register | POST | /user/registrations | 接口 success=true |
| oauth | oauth | POST | /oauth2/token | notEmpty:result.access_token |

