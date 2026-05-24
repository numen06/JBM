# OAuth2 core - 业务测试报告

- 时间: 2026-05-25 03:17:24
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 5/5

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-02 | password token | POST | 200 | PASS | 1.181 | notEmpty:result.access_token |  |
| TC-AUTH-05 | userinfo | GET | 200 | PASS | 0.321 | notNull:result.userId; isList:result.roles; isList:result.menuPermission |  |
| TC-AUTH-04 | refresh | POST | 500 | PASS | 0.181 | - | 无效client_secret: jbmSeedDevSecret0000000001 |
| TC-AUTH-12 | renewal | POST | 200 | PASS | 0.388 | - |  |
| TC-AUTH-13 | diagnose | GET | 200 | PASS | 0.260 | contains:result.诊断结论:双层Token均有效; ttlMaxDelta:result.1_Sa-Token层.token_TTL(秒):result.2_OAuth2层.access_token_TTL(秒):120 |  |
