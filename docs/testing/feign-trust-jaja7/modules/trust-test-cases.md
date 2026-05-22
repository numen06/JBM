# Feign trust - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-TRUST-01 client token ok

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| routes | routes | GET | /gateway/api/route | isTrue:success |

## TC-TRUST-02 no token

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| no auth | no auth | GET | /gateway/api/route | 接口 success=true |

## TC-TRUST-03 bad token

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| bad | bad | GET | /gateway/api/route | 接口 success=true |

## TC-TRUST-04 id token only

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| id only | id only | GET | /gateway/api/route | 接口 success=true |

