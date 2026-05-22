# 角色 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-ROLE-01 角色：创建-查询-修改-删除闭环

**前置条件**：tenantId=0；可写角色表

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 创建角色 | 创建角色 | POST | /role | notNull:result |
| 查询详情校验编码 | 查询详情校验编码 | GET | /role/{roleId} | eq:result.roleCode:{roleCode}; eq:result.roleName:REST角色 |
| 修改角色名称 | 修改角色名称 | PUT | /role/{roleId} | 接口 success=true |
| 再次查询校验修改 | 再次查询校验修改 | GET | /role/{roleId} | eq:result.roleName:REST角色-已改 |
| 删除角色 | 删除角色 | DELETE | /role/{roleId} | 接口 success=true |
| 删除后详情应为空 | 删除后详情应为空 | GET | /role/{roleId} | isNull:result |

## TC-ROLE-02 角色：全量列表结构（允许空库）

**前置条件**：jaja7 可无种子数据

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| GET /role/all | GET /role/all | GET | /role/all | isList:result |

