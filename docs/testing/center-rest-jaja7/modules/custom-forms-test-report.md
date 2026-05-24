# 自定义表单 - 业务测试报告

- 时间: 2026-05-24 22:02:37
- 服务可用: 是
- 结果: **PASS**
- 步骤通过: 2/2

| 场景 | 步骤 | 方法 | HTTP | 结果 | 耗时(s) | 业务断言 | 备注 |
|------|------|------|------|------|---------|----------|------|
| TC-CF-01 | 保存表单 | POST | 200 | PASS | 0.277 | notNull:result.id |  |
| TC-CF-01 | 查询详情 | POST | 200 | PASS | 0.238 | eq:result.name:{formDisplayName}; eq:result.formOrTable:form |  |
