# Feign trust - 业务测试报告

- 时间: 2026-05-25 03:36:01
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 6/6

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-01 | routes | GET | 200 | PASS | 0.055 | isTrue:success |  |
| TC-TRUST-02 | no auth | GET | 400 | PASS | 0.027 | - | HTTP Error 400: Bad Request |
| TC-TRUST-03 | bad | GET | 401 | PASS | 0.044 | - | HTTP Error 401: Unauthorized |
| TC-TRUST-04 | service token fake | GET | 400 | PASS | 0.018 | - | HTTP Error 400: Bad Request |
| TC-TRUST-05 | current user | GET | 200 | PASS | 0.328 | isTrue:success; notNull:result.userId |  |
| TC-TRUST-06 | routes service-token | GET | 200 | PASS | 0.048 | isTrue:success |  |
