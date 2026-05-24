# Feign trust - 业务测试报告

- 时间: 2026-05-24 21:05:28
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 6/6

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-01 | routes | GET | 400 | PASS | 0.004 | isTrue:success | 缺少签名参数 |
| TC-TRUST-02 | no auth | GET | 400 | PASS | 0.004 | - | HTTP Error 400: Bad Request |
| TC-TRUST-03 | bad | GET | 401 | PASS | 0.039 | - | HTTP Error 401: Unauthorized |
| TC-TRUST-04 | id only fake | GET | 400 | PASS | 0.004 | - | HTTP Error 400: Bad Request |
| TC-TRUST-05 | current user | GET | 200 | PASS | 0.319 | isTrue:success; notNull:result.userId |  |
| TC-TRUST-06 | routes id-token | GET | 400 | PASS | 0.017 | isTrue:success | 缺少签名参数 |
