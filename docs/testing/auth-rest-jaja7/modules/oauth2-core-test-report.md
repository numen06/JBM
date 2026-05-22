# OAuth2 core - 业务测试报告

- 时间: 2026-05-22 11:56:35
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 3/7

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-02 | password token | POST | 500 | FAIL | 0.167 | notEmpty:result.access_token | 没有找到此用户 |
| TC-AUTH-05 | userinfo | GET | 500 | FAIL | 0.026 | notNull:result.userId; notEmpty:result.roles; notEmpty:result.menuPermission | 登录已过期，请重新登录 |
| TC-AUTH-04 | refresh | POST | 500 | FAIL | 0.033 | - | 无效refresh_token: {refreshToken} |
| TC-AUTH-12 | renewal | POST | 200 | FAIL | 0.006 | - | 续签失败：无效access_token：{accessToken} |
| TC-AUTH-13 | diagnose | GET | 500 | PASS | 0.027 | - | 接口异常! |
| TC-AUTH-06 | logout | DELETE | 200 | PASS | 0.009 | - | 接口异常! |
| TC-AUTH-06 | userinfo after logout | GET | 500 | PASS | 0.003 | - | HTTP Error 500:  |
