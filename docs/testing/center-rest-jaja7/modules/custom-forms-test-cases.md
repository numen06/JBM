# 自定义表单 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-CF-01 自定义表单：保存后详情一致

**前置条件**：custom_forms 表可写

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 保存表单 | 保存表单 | POST | /customForms/saveData | notNull:result.id |
| 查询详情 | 查询详情 | POST | /customForms/getDetail | eq:result.name:{formDisplayName}; eq:result.formOrTable:form |

