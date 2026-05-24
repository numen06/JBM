# 扩展表单 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-EXT-01 扩展表单：保存发布-Redis-库表一致

**前置条件**：Redis 可用；extend_field 已配置

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 保存并发布 | 保存并发布 | POST | /extend-field/forms/cen_form_{ts} | eq:result.formCode:{formCode}; sizeGte:result.fields:1; eq:result.fields.0.fieldName:note |
| Redis 字段定义 | Redis 字段定义 | GET | /extend-field/forms/{formCode}/definitions | sizeGte:result:1; eq:result.0.fieldName:note |
| 库表定义 | 库表定义 | GET | /extend-field/forms/{formCode} | eq:result.formCode:{formCode}; eq:result.formName:CEN_FORM |

