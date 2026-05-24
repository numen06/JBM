# 其它 - 业务测试报告

- 时间: 2026-05-24 21:05:27
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 1/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-MISC-01 | 字典 Map | GET | 400 | FAIL | 0.004 | notEmpty:result | 缺少签名参数 |
| TC-MISC-01 | API 列表 | GET | 400 | FAIL | 0.004 | isList:result | 缺少签名参数 |
| TC-MISC-02 | POST /baseOrg/root | POST | 400 | PASS | 0.005 | isList:result | 缺少签名参数 |
