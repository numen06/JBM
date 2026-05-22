# login lock - 业务测试报告

- 时间: 2026-05-22 11:56:35
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 8/10

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-07 | wrong 1 | POST | 500 | PASS | 0.099 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 2 | POST | 500 | PASS | 0.093 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 3 | POST | 500 | PASS | 0.089 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 4 | POST | 500 | PASS | 0.073 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 5 | POST | 500 | PASS | 0.087 | - | HTTP Error 500:  |
| TC-AUTH-07 | locked | POST | 500 | FAIL | 0.086 | contains:message:超限 | contains:message:超限 实际='请求地址发生服务器错误' |
| TC-AUTH-11 | wrong 1 | POST | 500 | PASS | 0.090 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 2 | POST | 500 | PASS | 0.064 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 3 | POST | 500 | PASS | 0.076 | - | HTTP Error 500:  |
| TC-AUTH-11 | correct login | POST | 500 | FAIL | 0.077 | - | 请求地址发生服务器错误 |
