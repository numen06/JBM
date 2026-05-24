# 专用测试用户准备 - 业务测试报告

- 时间: 2026-05-25 00:11:41
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-UP-00 | register | POST | 200 | PASS | 0.165 | - |  |
| TC-UP-00 | oauth password | POST | 200 | PASS | 0.954 | notEmpty:result.access_token |  |
| TC-UP-00 | userinfo | GET | 200 | PASS | 0.230 | notNull:result.userId; isList:result.roles |  |
