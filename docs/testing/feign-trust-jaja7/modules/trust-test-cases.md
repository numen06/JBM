# Feign trust - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-TRUST-01 client token ok

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| routes | routes | GET | /role/all | isTrue:success |

## TC-TRUST-02 no token

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| no auth | no auth | GET | /role/all | 接口 success=true |

## TC-TRUST-03 bad token

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| bad | bad | GET | /role/all | 接口 success=true |

## TC-TRUST-04 id token only invalid

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| id only fake | id only fake | GET | /role/all | 接口 success=true |

## TC-TRUST-05 user token current user

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| current user | current user | GET | /current/user | isTrue:success; notNull:result.userId |

## TC-TRUST-06 valid id token internal

**前置条件**：-

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| routes id-token | routes id-token | GET | /role/all | isTrue:success |

