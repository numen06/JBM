# login lock - 业务场景用例

说明：每场景含前置条件、步骤与**业务断言**（非仅 HTTP 200）。

## TC-AUTH-07 lock after 5 fails

**前置条件**：

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| wrong 1 | wrong 1 | POST | /oauth2/token | 接口 success=true |
| wrong 2 | wrong 2 | POST | /oauth2/token | 接口 success=true |
| wrong 3 | wrong 3 | POST | /oauth2/token | 接口 success=true |
| wrong 4 | wrong 4 | POST | /oauth2/token | 接口 success=true |
| wrong 5 | wrong 5 | POST | /oauth2/token | 接口 success=true |
| locked | locked | POST | /oauth2/token | contains:message:超限 |

## TC-AUTH-11 clear error count

**前置条件**：

| 步骤 | 操作 | 方法 | 路径 | 业务断言 |
|------|------|------|------|----------|
| wrong 1 | wrong 1 | POST | /oauth2/token | 接口 success=true |
| wrong 2 | wrong 2 | POST | /oauth2/token | 接口 success=true |
| wrong 3 | wrong 3 | POST | /oauth2/token | 接口 success=true |
| correct login | correct login | POST | /oauth2/token | 接口 success=true |

