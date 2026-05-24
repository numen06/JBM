# 扩展表单 - 业务测试报告

- 时间: 2026-05-25 00:11:32
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 3/3

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-EXT-01 | 保存并发布 | POST | 200 | PASS | 0.261 | eq:result.formCode:{formCode}; sizeGte:result.fields:1; eq:result.fields.0.fieldName:note |  |
| TC-EXT-01 | Redis 字段定义 | GET | 200 | PASS | 0.120 | sizeGte:result:1; eq:result.0.fieldName:note |  |
| TC-EXT-01 | 库表定义 | GET | 200 | PASS | 0.135 | eq:result.formCode:{formCode}; eq:result.formName:CEN_FORM |  |
