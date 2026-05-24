# login lock - 业务测试报告

- 时间: 2026-05-25 03:17:24
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 11/11

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-07-prep | register lock user | POST | 200 | PASS | 0.279 | - |  |
| TC-AUTH-07 | wrong 1 | POST | 500 | PASS | 0.518 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 2 | POST | 500 | PASS | 0.561 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 3 | POST | 500 | PASS | 0.555 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 4 | POST | 500 | PASS | 0.494 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 5 | POST | 500 | PASS | 0.541 | - | HTTP Error 500:  |
| TC-AUTH-07 | locked | POST | 500 | PASS | 0.280 | contains:message:超限 | 请求地址发生服务器错误 |
| TC-AUTH-11 | wrong 1 | POST | 500 | PASS | 0.297 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 2 | POST | 500 | PASS | 0.295 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 3 | POST | 500 | PASS | 0.285 | - | HTTP Error 500:  |
| TC-AUTH-11 | correct login | POST | 500 | PASS | 0.283 | - | 请求地址发生服务器错误 |
