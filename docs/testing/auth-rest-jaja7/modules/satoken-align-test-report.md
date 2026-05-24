# Sa-Token OAuth2 align - 业务测试报告

- 时间: 2026-05-24 22:02:27
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 4/4

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-14 | diagnose config | GET | 200 | PASS | 0.122 | - |  |
| TC-AUTH-15 | password token | POST | 200 | PASS | 1.107 | - |  |
| TC-AUTH-15 | renewal | POST | 200 | PASS | 0.307 | - |  |
| TC-AUTH-15 | diagnose after renewal | GET | 200 | PASS | 0.234 | contains:result.诊断结论:双层Token均有效; ttlMaxDelta:result.1_Sa-Token层.token_TTL(秒):result.2_OAuth2层.access_token_TTL(秒):120 |  |
