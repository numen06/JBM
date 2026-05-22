# Feign trust - 业务测试报告

- 时间: 2026-05-22 18:49:24
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 5/6

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-01 | routes | GET | 401 | PASS | 0.072 | isTrue:success | 服务认证失败，无法访问系统资源 |
| TC-TRUST-02 | no auth | GET | 401 | PASS | 0.004 | - | HTTP Error 401:  |
| TC-TRUST-03 | bad | GET | 401 | PASS | 0.004 | - | HTTP Error 401:  |
| TC-TRUST-04 | id only fake | GET | 401 | PASS | 0.003 | - | HTTP Error 401:  |
| TC-TRUST-05 | current user | GET | 401 | FAIL | 0.035 | isTrue:success; notNull:result.userId | 服务认证失败，无法访问系统资源 |
| TC-TRUST-06 | routes id-token | GET | 401 | PASS | 0.026 | isTrue:success | 服务认证失败，无法访问系统资源 |
