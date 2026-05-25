# 其它 - 业务测试报告

- 时间: 2026-05-25 03:35:52
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-MISC-01 | 字典 Map | GET | 200 | PASS | 0.226 | notEmpty:result |  |
| TC-MISC-01 | API 列表 | GET | 200 | PASS | 0.575 | isList:result |  |
| TC-MISC-02 | POST /baseOrg/root | POST | 200 | PASS | 0.441 | isList:result |  |
