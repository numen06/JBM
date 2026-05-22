# 网关配置 - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-GW-01 网关：路由/限流分页可读

**前置条件**：网关配置表可访问

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| 路由分页 | 路由分页 | GET | /gateway/routes | notNull:result.total; isList:result.contents |
| IP 限流分页 | IP 限流分页 | GET | /gateway/limit/ip | notNull:result.total |
| 速率限流分页 | 速率限流分页 | GET | /gateway/limit/rate | notNull:result.total |

