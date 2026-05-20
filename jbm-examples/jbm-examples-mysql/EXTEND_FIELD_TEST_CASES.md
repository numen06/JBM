# 扩展字段联调 — 完整测试用例（请求 / 预期）

> **模块**：`jbm-examples-mysql`  
> **Profile**：`h2`（内存库 + Liquibase）  
> **字段定义来源**：`LOCAL`（`application-h2.yml`，无 Redis）  
> **自动化**：`MicroMysqlExtendFieldFullFlowIT#fullExtendFieldFlow_localMode`  
> **运行**：`mvn test -pl jbm-examples/jbm-examples-mysql -Pbuild-examples "-Dtest=MicroMysqlExtendFieldFullFlowIT"`  
> **说明**：根目录 `mvn deploy` 默认跳过 `jbm-examples` 的编译与上传；本地跑示例/测试需加 `-Pbuild-examples`。

---

## 0. 前置条件

| 项 | 值 |
|----|-----|
| 应用 | `MicroMysqlApplication`，`@EnableExtendField(source = LOCAL)` |
| 表 | `md_extend_demo`（含 `extend_data` CLOB/JSON） |
| 表单编码 | `extend_demo_form` |
| 已注册扩展字段 | `contactPhone`（string）、`region`（string） |
| 多租户 | `md_extend_demo` 在 `DemoTenantLineHandler.ignoreTable` 中，**不**追加 `tenant_id` |
| 通用请求头 | `Content-Type: application/json` |
| 响应包装 | 统一 `ResultBody`：`success`、`code`、`message`、`result` |

**Base URL**：`http://{host}:{port}`（集成测试为随机端口，下文用 `{base}` 表示）

---

## 1. 配置（只读参考）

```yaml
jbm:
  extend-field:
    enabled: true
    auto-flatten: true
    source: LOCAL
    sync-local-to-redis-on-startup: false
    definitions:
      extend_demo_form:
        fields:
          - fieldName: contactPhone
            fieldType: string
            fieldLabel: 联系电话
          - fieldName: region
            fieldType: string
            fieldLabel: 区域
```

---

## 2. 用例列表

| ID | 名称 | 方法 | 路径 |
|----|------|------|------|
| TC-EF-01 | 读取 LOCAL 字段定义 | GET | `/api/extend-field/definitions/extend_demo_form` |
| TC-EF-02 | formCode 平铺创建 + 响应平铺 | POST | `/api/h2/mp/extend-demos` |
| TC-EF-03 | 按 ID 查询（响应平铺） | GET | `/api/h2/mp/extend-demos/{id}` |
| TC-EF-04 | 直接提交 extendData 创建 | POST | `/api/h2/mp/extend-demos` |
| TC-EF-05 | 列表查询（每行平铺） | GET | `/api/h2/mp/extend-demos` |
| TC-EF-06 | 条件检索（extend 键） | POST | `/api/h2/mp/extend-demos/search` |
| TC-EF-07 | 条件检索（extendQuery 键） | POST | `/api/h2/mp/extend-demos/search` |
| TC-EF-08 | 条件检索无匹配 | POST | `/api/h2/mp/extend-demos/search` |
| TC-EF-09 | 无 Redis 时写定义失败 | POST | `/api/extend-field/definitions` |

---

### TC-EF-01 读取 LOCAL 字段定义

**目的**：确认 YAML 定义已加载，管理读接口可用。

**请求**

```http
GET {base}/api/extend-field/definitions/extend_demo_form
Accept: application/json
```

**预期 HTTP**：`200 OK`

**预期响应体**（结构，字段名必须包含）：

```json
{
  "success": true,
  "code": 0,
  "message": "成功",
  "result": [
    {
      "fieldName": "contactPhone",
      "fieldType": "string",
      "fieldLabel": "联系电话"
    },
    {
      "fieldName": "region",
      "fieldType": "string",
      "fieldLabel": "区域"
    }
  ]
}
```

**断言要点**

- `result` 为数组，长度 ≥ 2
- 存在 `fieldName=contactPhone` 与 `fieldName=region`

---

### TC-EF-02 formCode 平铺创建 + 响应平铺

**目的**：`ExtendFieldRequestBodyAdvice` 按 `formCode` 将平铺字段拆入 `extendData` 入库；`ResultExtendAop` 响应平铺。

**请求**

```http
POST {base}/api/h2/mp/extend-demos
Content-Type: application/json
```

```json
{
  "formCode": "extend_demo_form",
  "bizCode": "EXT-FLOW-001",
  "title": "formCode 拆分创建",
  "contactPhone": "13800138000",
  "region": "华东"
}
```

**Advice 处理后（Controller 收到的逻辑体，仅说明）**

```json
{
  "bizCode": "EXT-FLOW-001",
  "title": "formCode 拆分创建",
  "extendData": {
    "contactPhone": "13800138000",
    "region": "华东"
  }
}
```

**预期 HTTP**：`200 OK`

**预期响应体**（`result` 为单条记录，**已平铺**）：

```json
{
  "success": true,
  "code": 0,
  "result": {
    "id": 1,
    "bizCode": "EXT-FLOW-001",
    "title": "formCode 拆分创建",
    "contactPhone": "13800138000",
    "region": "华东"
  }
}
```

**说明**

- `id` 为自增，具体数值 > 0 即可
- **不得**出现顶层 `extendData`、`formCode`
- **必须**出现平铺的 `contactPhone`、`region`

**预期数据库**（表 `md_extend_demo`，`biz_code='EXT-FLOW-001'`）

| 列 | 预期 |
|----|------|
| `biz_code` | `EXT-FLOW-001` |
| `title` | `formCode 拆分创建` |
| `extend_data`（JSON） | `{"contactPhone":"13800138000","region":"华东"}` |

**保存变量**：`idFormCodeCreate = result.id`（供 TC-EF-03、06、08 使用）

---

### TC-EF-03 按 ID 查询（响应平铺）

**前置**：TC-EF-02 已执行，已知 `idFormCodeCreate`

**请求**

```http
GET {base}/api/h2/mp/extend-demos/{idFormCodeCreate}
Accept: application/json
```

**预期 HTTP**：`200 OK`

**预期响应体**

```json
{
  "success": true,
  "code": 0,
  "result": {
    "id": 1,
    "bizCode": "EXT-FLOW-001",
    "title": "formCode 拆分创建",
    "contactPhone": "13800138000",
    "region": "华东"
  }
}
```

**断言要点**

- `result.region` = `华东`
- **无** `extendData` 字段

---

### TC-EF-04 直接提交 extendData 创建

**目的**：不经过 formCode，直接持久化 `extendData`。

**请求**

```http
POST {base}/api/h2/mp/extend-demos
Content-Type: application/json
```

```json
{
  "bizCode": "EXT-FLOW-002",
  "title": "直接 extendData",
  "extendData": {
    "contactPhone": "13900001111",
    "region": "华南"
  }
}
```

**预期 HTTP**：`200 OK`

**预期响应体**

```json
{
  "success": true,
  "code": 0,
  "result": {
    "id": 2,
    "bizCode": "EXT-FLOW-002",
    "title": "直接 extendData",
    "contactPhone": "13900001111",
    "region": "华南"
  }
}
```

**预期数据库**

| `biz_code` | `extend_data` |
|------------|----------------|
| `EXT-FLOW-002` | `{"contactPhone":"13900001111","region":"华南"}` |

**保存变量**：`idExplicitExtend = result.id`

---

### TC-EF-05 列表查询（每行平铺）

**请求**

```http
GET {base}/api/h2/mp/extend-demos
Accept: application/json
```

**预期 HTTP**：`200 OK`

**预期响应体**（片段）

```json
{
  "success": true,
  "code": 0,
  "result": [
    {
      "bizCode": "EXT-FLOW-001",
      "contactPhone": "13800138000",
      "region": "华东"
    },
    {
      "bizCode": "EXT-FLOW-002",
      "contactPhone": "13900001111",
      "region": "华南"
    }
  ]
}
```

**断言要点**

- `result` 数组中至少一条 `bizCode=EXT-FLOW-001` 的记录
- 该记录 **无** `extendData`，**有** `region`

---

### TC-EF-06 条件检索（extend 键）

**目的**：请求体 `extend` 由 Advice 映射为 `extendQuery`（演示 Controller 同时兼容两种键）。

**请求**

```http
POST {base}/api/h2/mp/extend-demos/search
Content-Type: application/json
```

```json
{
  "bizCode": "EXT-FLOW-001",
  "extend": {
    "region": "华东"
  }
}
```

**预期 HTTP**：`200 OK`

**预期响应体**

```json
{
  "success": true,
  "code": 0,
  "result": [
    {
      "bizCode": "EXT-FLOW-001",
      "region": "华东",
      "contactPhone": "13800138000"
    }
  ]
}
```

**断言要点**

- `result.length` = 1
- `result[0].bizCode` = `EXT-FLOW-001`

---

### TC-EF-07 条件检索（extendQuery 键）

**请求**

```http
POST {base}/api/h2/mp/extend-demos/search
Content-Type: application/json
```

```json
{
  "bizCode": "EXT-FLOW-002",
  "extendQuery": {
    "region": "华南"
  }
}
```

**预期 HTTP**：`200 OK`

**预期响应体**

```json
{
  "success": true,
  "code": 0,
  "result": [
    {
      "bizCode": "EXT-FLOW-002",
      "region": "华南"
    }
  ]
}
```

**断言要点**：`result.length` = 1

---

### TC-EF-08 条件检索无匹配

**请求**

```http
POST {base}/api/h2/mp/extend-demos/search
Content-Type: application/json
```

```json
{
  "bizCode": "EXT-FLOW-001",
  "extend": {
    "region": "不存在"
  }
}
```

**预期 HTTP**：`200 OK`

**预期响应体**

```json
{
  "success": true,
  "code": 0,
  "result": []
}
```

**断言要点**：`result` 为空数组

---

### TC-EF-09 无 Redis 时写定义失败（LOCAL 模式）

**目的**：`FieldDefinitionWriter` 未装配时，POST 定义应业务失败而非 500。

**请求**

```http
POST {base}/api/extend-field/definitions
Content-Type: application/json
```

```json
{
  "formCode": "new_form",
  "definitions": [
    {
      "fieldName": "x",
      "fieldType": "string",
      "fieldLabel": "测试"
    }
  ]
}
```

**预期 HTTP**：`200 OK`（框架统一包装，非 4xx）

**预期响应体**

```json
{
  "success": false,
  "code": 500,
  "message": "Redis 未配置，无法保存字段定义",
  "result": null
}
```

**断言要点**

- `success` = `false`
- `message` 包含 `Redis`

---

## 3. 端到端流程（执行顺序）

```text
TC-EF-01 → TC-EF-02 → TC-EF-03 → TC-EF-04 → TC-EF-05
    → TC-EF-06 → TC-EF-07 → TC-EF-08 → TC-EF-09
```

---

## 4. 生产环境差异（非本模块自动化范围）

| 项 | H2 示例 | 生产建议 |
|----|---------|----------|
| 字段定义 | `source: LOCAL` YAML | `source: REDIS` + `jbm-framework-autoconfigure-redis` |
| 条件检索 | 内存过滤 | `CommonMapper.xml` 中 `extendDataQuery` JSON 路径 |
| Redis Key | 无 | `extend_field:form:{formCode}`、`extend_field:names:{formCode}` |

---

## 5. 测试结果记录

| 执行时间 | 命令 | 用例数 | 失败 | 错误 | 结果 |
|----------|------|--------|------|------|------|
| 2026-05-19 | `mvn test -pl jbm-examples/jbm-examples-mysql "-Dtest=MicroMysqlExtendField*IT"` | **4**（全流程 1 + 单场景 3） | 0 | 0 | **BUILD SUCCESS** |

**自动化映射**

| 用例 ID | 自动化 |
|---------|--------|
| TC-EF-01～09 | `MicroMysqlExtendFieldFullFlowIT#fullExtendFieldFlow_localMode` |
| （单场景回归） | `MicroMysqlExtendFieldIT` 三个方法 |

**运行中修复**

| 问题 | 现象 | 修复 |
|------|------|------|
| LOCAL YAML 字段类型 | POST 500，`JSONObject cannot be cast to FieldDefinition` | `LocalFieldDefinitionService.toFieldDefinition()` 统一转换 |
| 多租户列 | INSERT 含 `tenant_id` 列不存在 | `DemoTenantLineHandler` 忽略 `md_extend_demo` |
| 无 Redis 写定义 | 需明确业务失败 | 断言 `success=false` 且 `message` 含 `Redis` |
