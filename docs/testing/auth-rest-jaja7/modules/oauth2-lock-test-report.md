# login lock - 业务测试报告

- 时间: 2026-05-22 23:33:35
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 10/11

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-07-prep | register lock user | POST | 200 | PASS | 0.208 | - |  |
| TC-AUTH-07 | wrong 1 | POST | 500 | PASS | 0.190 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 2 | POST | 500 | PASS | 0.166 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 3 | POST | 500 | PASS | 0.351 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 4 | POST | 500 | PASS | 0.178 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 5 | POST | 500 | PASS | 0.314 | - | HTTP Error 500:  |
| TC-AUTH-07 | locked | POST | 200 | FAIL | 0.238 | contains:message:超限 | contains:message:超限 实际='成功' |
| TC-AUTH-11 | wrong 1 | POST | 500 | PASS | 0.172 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 2 | POST | 500 | PASS | 0.175 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 3 | POST | 500 | PASS | 0.194 | - | HTTP Error 500:  |
| TC-AUTH-11 | correct login | POST | 200 | PASS | 0.384 | - |  |
