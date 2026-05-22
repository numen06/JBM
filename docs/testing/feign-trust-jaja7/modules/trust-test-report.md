# Feign trust - 业务测试报告

- 时间: 2026-05-22 22:01:40
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 6/6

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-TRUST-01 | routes | GET | 404 | PASS | 0.009 | isTrue:success | 无效的访问地址 |
| TC-TRUST-02 | no auth | GET | 404 | PASS | 0.009 | - | HTTP Error 404: Not Found |
| TC-TRUST-03 | bad | GET | 404 | PASS | 0.019 | - | HTTP Error 404: Not Found |
| TC-TRUST-04 | id only fake | GET | 404 | PASS | 0.024 | - | HTTP Error 404: Not Found |
| TC-TRUST-05 | current user | GET | 200 | PASS | 0.055 | isTrue:success; notNull:result.userId |  |
| TC-TRUST-06 | routes id-token | GET | 404 | PASS | 0.005 | isTrue:success | 无效的访问地址 |
