# 用户 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-USER-01 用户：注册-按用户名查询-详情-角色

**前置条件**：无同名用户 u_{usuffix}；tenantId=0

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 注册 | 注册 | POST | /user/registrations | 接口 success=true |
| 按 userName 查询 | 按 userName 查询 | GET | /user | sizeGte:result:1; eq:result.0.userName:{testUserName} |
| 用户详情 | 用户详情 | GET | /user/{userId} | eq:result.userName:{testUserName}; notNull:result.userId |
| 用户角色列表 | 用户角色列表 | GET | /user/{userId}/roles | isList:result |

