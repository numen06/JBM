# Feign test user - 业务测试报告

- 时间: 2026-05-25 03:17:45
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 2/2

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-00 | register | POST | 200 | PASS | 0.198 | - |  |
| TC-TRUST-00 | oauth | POST | 200 | PASS | 1.058 | notEmpty:result.access_token |  |
