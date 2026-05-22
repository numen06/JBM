# Feign trust - 业务测试报告

- 时间: 2026-05-22 12:39:31
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 3/4

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-01 | routes | GET | 401 | FAIL | 0.024 | isTrue:success | 无效Token |
| TC-TRUST-02 | no auth | GET | 401 | PASS | 0.004 | - | HTTP Error 401:  |
| TC-TRUST-03 | bad | GET | 401 | PASS | 0.067 | - | HTTP Error 401:  |
| TC-TRUST-04 | id only | GET | 401 | PASS | 0.020 | - | HTTP Error 401:  |
