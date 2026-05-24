# OAuth2 core - 业务测试报告

- 时间: 2026-05-24 20:26:07
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 1/5（跳过 1）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-02 | password token | POST | 0 | SKIP | 0.000 |  | 缺少上下文 {testUserName} |
| TC-AUTH-05 | userinfo | GET | 500 | FAIL | 0.026 | notNull:result.userId; isList:result.roles; isList:result.menuPermission | 登录已过期，请重新登录 |
| TC-AUTH-04 | refresh | POST | 500 | PASS | 0.032 | - | 无效refresh_token: {refreshToken} |
| TC-AUTH-12 | renewal | POST | 200 | FAIL | 0.031 | - | 续签失败：无效access_token：{accessToken} |
| TC-AUTH-13 | diagnose | GET | 200 | FAIL | 0.137 | contains:result.诊断结论:双层Token均有效; ttlMaxDelta:result.1_Sa-Token层.token_TTL(秒):result.2_OAuth2层.access_token_TTL(秒):120 | contains:result.诊断结论:双层Token均有效 实际='Token已完全过期（Sa-Token层和OAuth2层都不存在）' |
