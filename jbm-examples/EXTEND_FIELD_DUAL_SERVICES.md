# 扩展字段双服务联动示例

两个可独立运行的 Spring Boot 应用，演示「配置侧入库 + 发布 Redis」与「业务侧只读 Redis」的生产形态。

| 模块 | 端口 | 职责 |
|------|------|------|
| `jbm-examples-extendfield-designer` | **18081** | 表单定义写入 `md_extend_form_definition`，并 `publish` 到 Redis |
| `jbm-examples-extendfield-business` | **18082** | `source: REDIS`，按 `formCode` 拆分请求；订单值进 `md_extend_order.extend_data` |

二者共用 Redis（默认 `10.100.10.62:6379`，可用环境变量 `REDIS_HOST` / `REDIS_PORT` 覆盖）。

## 本地启动（两个终端）

```bash
# 终端 1：设计器
mvn -pl jbm-examples/jbm-examples-extendfield-designer spring-boot:run

# 终端 2：业务（需 Redis 中已有表单定义，或先执行下面「创建表单」）
mvn -pl jbm-examples/jbm-examples-extendfield-business spring-boot:run
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
mvn test -pl jbm-examples/jbm-examples-extendfield-business "-Dtest=ExtendFieldDualServicesIT"
```

## 与 `jbm-examples-mysql` 的关系

- `jbm-examples-mysql`：单应用内同时含设计器 + 业务（便于全链路 IT）。
- 本双模块：拆成两个进程，更贴近「配置微服务 + 业务微服务」部署方式。
