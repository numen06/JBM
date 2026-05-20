# 扩展字段双服务联动示例

两个可独立运行的 Spring Boot 应用，演示「配置侧入库 + 发布 Redis」与「业务侧只读 Redis」的生产形态。

| 模块 | 端口 | 职责 |
|------|------|------|
| `jbm-examples-extendfield-designer` | **18081** | 表单定义写入 `md_extend_form_definition`，并 `publish` 到 Redis |
| `jbm-examples-extendfield-business` | **18082** | `source: REDIS`，按 `formCode` 拆分请求；订单值进 `md_extend_order.extend_data` |

二者共用 Redis（默认 `10.100.10.62:6379`，可用环境变量 `REDIS_HOST` / `REDIS_PORT` 覆盖）。

## 多租户（相同接口、不同字段定义）

请求头 **`X-Demo-Tenant-Id`**（可配置为 `jbm.extend-field.tenant.header`）：

- 设计器：`md_extend_form_definition` 按 `(tenant_id, form_code)` 唯一；发布 Redis 键为 `extend_field:form:{tenantId}:{formCode}`
- 业务：同一 URL `POST /api/business/orders`，body 仍用 `formCode`；Advice 按**当前租户**读 Redis 拆分字段；订单表 `tenant_id` 行级隔离

示例：租户 `1001` 与 `2002` 可共用 `formCode=sales_form`，但字段列表互不影响。自动化见 `ExtendFieldDualTenantIT`。

### 无租户头 → 默认模块（`tenant_id = 0`）

未传 `X-Demo-Tenant-Id` 且 `use-default-when-missing: true`（默认开启）时：

- Redis 作用域：`extend_field:form:0:{formCode}`
- 库表：`tenant_id = 0`
- 接口 URL 与 body 中的 `formCode` **不变**

```yaml
jbm:
  extend-field:
    tenant:
      enabled: true
      default-tenant-id: "0"      # 默认模块，可按平台约定改为 platform / global
      use-default-when-missing: true
```

若希望「无头即失败」，设 `use-default-when-missing: false`。

## 本地启动（两个终端）

> 根目录 `mvn deploy` 默认跳过 `jbm-examples` 编译与 Maven 上传；本地 `run` / `test` 请加 **`-Pbuild-examples`**。

```bash
# 终端 1：设计器
mvn -pl jbm-examples/jbm-examples-extendfield-designer -Pbuild-examples spring-boot:run

# 终端 2：业务（需 Redis 中已有表单定义，或先执行下面「创建表单」）
mvn -pl jbm-examples/jbm-examples-extendfield-business -Pbuild-examples spring-boot:run
```

## 联动流程（curl）

### 1. 设计器：新建表单（入库 + Redis）

```http
POST http://localhost:18081/api/designer/forms/dual_sales_form
Content-Type: application/json

{
  "formName": "销售扩展表单",
  "fields": [
    { "fieldName": "contactPhone", "fieldType": "string", "fieldLabel": "联系电话" },
    { "fieldName": "region", "fieldType": "string", "fieldLabel": "区域" }
  ]
}
```

### 2. 业务：提交订单（平铺字段 + formCode）

```http
POST http://localhost:18082/api/business/orders
Content-Type: application/json

{
  "formCode": "dual_sales_form",
  "orderNo": "ORD-001",
  "title": "第一单",
  "contactPhone": "13800001111",
  "region": "华东"
}
```

响应中 `contactPhone`、`region` 被平铺；`extend_data` 在库内为 JSON。

### 3. 设计器：在线加字段

```http
PUT http://localhost:18081/api/designer/forms/dual_sales_form
```

body 中增加 `vipLevel` 字段后，业务侧**无需重启**，下一笔订单即可带 `vipLevel`。

### 4. Redis 丢失后：从库重新发布

```http
POST http://localhost:18081/api/designer/forms/dual_sales_form/publish
```

## 自动化联动测试

业务模块内 `ExtendFieldDualServicesIT`：JUnit 内先拉起设计器进程，再测业务 HTTP，需 Redis 可达：

```bash
mvn test -pl jbm-examples/jbm-examples-extendfield-business -am -Pbuild-examples "-Dtest=ExtendFieldDualServicesIT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 与 `jbm-examples-mysql` 的关系

- `jbm-examples-mysql`：单应用内同时含设计器 + 业务（便于全链路 IT）。
- 本双模块：拆成两个进程，更贴近「配置微服务 + 业务微服务」部署方式。
