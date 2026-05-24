# Feign trust - 业务测试报告

- 时间: 2026-05-25 03:17:45
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 5/6

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-01 | routes | GET | 200 | PASS | 0.062 | isTrue:success |  |
| TC-TRUST-02 | no auth | GET | 400 | PASS | 0.017 | - | HTTP Error 400: Bad Request |
| TC-TRUST-03 | bad | GET | 200 | FAIL | 0.073 | - | 成功 |
| TC-TRUST-04 | id only fake | GET | 400 | PASS | 0.019 | - | HTTP Error 400: Bad Request |
| TC-TRUST-05 | current user | GET | 200 | PASS | 0.294 | isTrue:success; notNull:result.userId |  |
| TC-TRUST-06 | routes id-token | GET | 200 | PASS | 0.038 | isTrue:success |  |
