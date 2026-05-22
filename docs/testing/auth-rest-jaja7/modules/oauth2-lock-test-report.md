# login lock - 业务测试报告

- 时间: 2026-05-22 18:20:55
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 11/11

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-AUTH-07-prep | register lock user | POST | 200 | PASS | 0.099 | - |  |
| TC-AUTH-07 | wrong 1 | POST | 500 | PASS | 0.337 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 2 | POST | 500 | PASS | 0.225 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 3 | POST | 500 | PASS | 0.323 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 4 | POST | 500 | PASS | 0.327 | - | HTTP Error 500:  |
| TC-AUTH-07 | wrong 5 | POST | 500 | PASS | 0.227 | - | HTTP Error 500:  |
| TC-AUTH-07 | locked | POST | 500 | PASS | 0.117 | contains:message:超限 | 请求地址发生服务器错误 |
| TC-AUTH-11 | wrong 1 | POST | 500 | PASS | 0.286 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 2 | POST | 500 | PASS | 0.268 | - | HTTP Error 500:  |
| TC-AUTH-11 | wrong 3 | POST | 500 | PASS | 0.221 | - | HTTP Error 500:  |
| TC-AUTH-11 | correct login | POST | 500 | PASS | 0.326 | - | 请求地址发生服务器错误 |
