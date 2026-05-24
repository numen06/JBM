# 扩展表单 - 业务测试报告

- 时间: 2026-05-24 21:05:27
- 服务可用: 是
- 结果: **FAIL**
- 步骤通过: 0/3（跳过 2）

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-EXT-01 | 保存并发布 | POST | 400 | FAIL | 0.003 | eq:result.formCode:{formCode}; sizeGte:result.fields:1; eq:result.fields.0.fieldName:note | 缺少签名参数 |
| TC-EXT-01 | Redis 字段定义 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {formCode} |
| TC-EXT-01 | 库表定义 | GET | 0 | SKIP | 0.000 |  | 缺少上下文 {formCode} |
