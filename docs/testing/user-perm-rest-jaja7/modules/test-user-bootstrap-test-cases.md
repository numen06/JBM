# 专用测试用户准备 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-UP-00 注册新用户并 OAuth 登录

**前置条件**：无同名用户 uperm_{usuffix}；Gateway+Auth 可用

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| register | register | POST | /user/registrations | 接口 success=true |
| resolve userId | resolve userId | GET | /user | sizeGte:result:1; eq:result.0.userName:{testUserName} |
| oauth password | oauth password | POST | /oauth2/token | notEmpty:result.access_token |
| userinfo | userinfo | GET | /oauth2/userinfo | notNull:result.userId; isList:result.roles |

