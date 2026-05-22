# 其它 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-MISC-01 字典与 API 分页

**前置条件**：tenantId=0

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 字典 Map | 字典 Map | GET | /baseDic/getDicMap | notEmpty:result |
| API 列表 | API 列表 | GET | /api | isList:result |

## TC-MISC-02 组织根节点（无种子时可选）

**前置条件**：base_org 可能为空

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| POST /baseOrg/root | POST /baseOrg/root | POST | /baseOrg/root | isList:result |

