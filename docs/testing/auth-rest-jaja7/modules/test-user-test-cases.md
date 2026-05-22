# 专用测试用户 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-01 注册并 OAuth 登录

**前置条件**：Gateway+Auth 可用；无同名 uauth_{usuffix}

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| register | register | POST | /user/registrations | 接口 success=true |
| resolve userId | resolve userId | GET | /user | 接口 success=true |

