# 其它 - 业务测试报告

- 时间: 2026-05-22 18:21:01
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-MISC-01 | 字典 Map | GET | 200 | PASS | 0.074 | notEmpty:result |  |
| TC-MISC-01 | API 列表 | GET | 200 | PASS | 0.639 | isList:result |  |
| TC-MISC-02 | POST /baseOrg/root | POST | 200 | PASS | 0.036 | isList:result | 查询树根节点列表失败 |
