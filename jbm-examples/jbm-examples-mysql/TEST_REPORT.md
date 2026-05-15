# jbm-examples-mysql 集成测试报告

> 测试命令：`mvn test -pl jbm-examples/jbm-examples-mysql`
> 最近一次结果：**BUILD SUCCESS** · `Tests run: 14, Failures: 0, Errors: 0`
> 测试执行时间：2026-05-15（最近一次完整通过：`agent-tools/7a282e5f-012d-4104-bc40-35ccd7c723d7.txt`）

---

## 1. 实体 / POJO 概览

| 实体 | 表名 | 关键特性 | DDL 版本 |
|------|------|----------|----------|
| `MdSample` | `md_sample` | 普通 MyBatis-Plus 实体，`IdType.AUTO`，`form_json` 映射为 `String` | V2 |
| `MdFormRow` | `md_form_row` | JSON 列（`payload CLOB`），`JacksonTypeHandler` 映射为 `Map<String,Object>`，`autoResultMap=true` | V1 |
| `MdTenantDemo` | `md_tenant_demo` | 多租户，`tenant_id` 由拦截器维护；`remark` 列由 Liquibase V4 追加 | V3 + V4 |

---

## 2. 各 POJO 特性覆盖矩阵

| # | POJO / 特性 | 对应测试用例 | 所在测试类 | 覆盖方式 |
|---|-------------|-------------|-----------|----------|
| 1 | `MdSample` — Mapper `insert` + `getById` | `mdSample_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | `@Autowired MdSampleMapper`，`insert` → `getById` |
| 2 | `MdSample` — Service CRUD | `mdSample_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | `@Autowired MdSampleService`，`getById` |
| 3 | `MdSample` — HTTP POST 建数据 | `mdSample_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | `TestRestTemplate.postForEntity /api/h2/mp/samples` |
| 4 | `MdSample` — HTTP GET list | `mpSampleControllerCreatesAndListsMdSample` | `MicroMysqlH2ControllerToDatabaseIT` | `TestRestTemplate.getForEntity` |
| 5 | `MdSample` — JSON 序列化 / 反序列化 | `dtosAndEntities_roundTripJson` | `MicroMysqlDemoFullStackIT` | `ObjectMapper.writeValueAsBytes / readValue` |
| 6 | `MdFormRow` — `JacksonTypeHandler` 存 `Map` | `mdFormRow_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | `insert` → `getById`，校验 `payload.get("tag")` |
| 7 | `MdFormRow` — `autoResultMap` 读出 `Map` | `mdFormRow_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | `mdFormRowService.getById` 后读取 `payload` |
| 8 | `MdFormRow` — HTTP POST 建 JSON 数据 | `mdFormRow_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | `TestRestTemplate.postForEntity /api/h2/mp/form-rows` |
| 9 | `MdFormRow` — HTTP GET 单条 | `mdFormRowJsonFieldFullCrudUsesDtos` | `MicroMysqlH2ControllerToDatabaseIT` | `getForEntity /{id}` |
| 10 | `MdFormRow` — HTTP PUT 全量替换 payload | `mdFormRow_mapperThenServiceThenHttp`<br>`mdFormRowJsonFieldFullCrudUsesDtos` | `MicroMysqlDemoFullStackIT`<br>`MicroMysqlH2ControllerToDatabaseIT` | `TestRestTemplate.exchange(HttpMethod.PUT)` |
| 11 | `MdFormRow` — HTTP DELETE | `mdFormRowJsonFieldFullCrudUsesDtos` | `MicroMysqlH2ControllerToDatabaseIT` | `restTemplate.delete` → 再次 GET 验证 404 |
| 12 | `MdFormRow` — 空 payload 允许 | `mdFormRowCreateAllowsEmptyPayloadInDto` | `MicroMysqlH2ControllerToDatabaseIT` | `payload = Collections.emptyMap()` POST |
| 13 | `MdFormRow` — HTTP GET list | `mdFormRowListReturnsDtoArray`<br>`mdFormRow_mapperThenServiceThenHttp` | `MicroMysqlH2ControllerToDatabaseIT`<br>`MicroMysqlDemoFullStackIT` | `getForEntity` 返回数组长度 ≥ 1 |
| 14 | `MdFormRow` — JSON 序列化 / 反序列化 | `dtosAndEntities_roundTripJson` | `MicroMysqlDemoFullStackIT` | 8 个 DTO / 实体全部 `writeValueAsBytes` → `readValue` |
| 15 | `MdTenantDemo` — Mapper `insert` 回写 `id` | `mdTenantDemo_mapperThenServiceThenHttp_withTenantHeader` | `MicroMysqlDemoFullStackIT` | `insert` → `assertNotNull(id)` |
| 16 | `MdTenantDemo` — `tenant_id` 由拦截器注入（回读验证） | `mdTenantDemo_mapperThenServiceThenHttp_withTenantHeader` | `MicroMysqlDemoFullStackIT` | `selectById` 后 `assertNotNull(tenantId)` |
| 17 | `MdTenantDemo` — Service `save` + `getById` | `mdTenantDemo_serviceSaveAndGet` | `MicroMysqlAdvancedFeaturesIT` | `@Autowired MdTenantDemoService`，`save` → `getById` |
| 18 | `MdTenantDemo` — HTTP POST（租户头） | `mdTenantDemo_httpUsesSlaveDsAppContext_andTenantHeader` | `MicroMysqlAdvancedFeaturesIT` | `X-Demo-Tenant-Id: 500` → 响应中 `tenantId == 500L` |
| 19 | `MdTenantDemo` — HTTP GET list（租户隔离） | `mdTenantDemo_mapperThenServiceThenHttp_withTenantHeader` | `MicroMysqlDemoFullStackIT` | 带租户头 GET list，验证返回非空 |
| 20 | `MdTenantDemo` — Liquibase V4 `remark` 列可写 | `liquibaseV4RemarkColumnWritable` | `MicroMysqlAdvancedFeaturesIT` | `insert` 时写 `remark`，`selectById` 读回一致 |
| 21 | `MdTenantDemo` — JSON 序列化 / 反序列化 | `dtosAndEntities_roundTripJson` | `MicroMysqlDemoFullStackIT` | `CreateMdTenantDemoRequest`、`MdTenantDemoResponse`、`MdTenantDemo` 全部往返 |
| 22 | 多租户拦截 — `ignoreTable` 不加 `tenant_id` | `tenantLineFiltersSelectAcrossTenants` | `MicroMysqlAdvancedFeaturesIT` | `md_sample` 表名在 `ignoreTable` 中，切换租户后 COUNT 不变 |
| 23 | 多租户拦截 — `tenant_id` 条件追加 | `tenantLineFiltersSelectAcrossTenants` | `MicroMysqlAdvancedFeaturesIT` | 切 `tenantId=100` 插入 → 切 `200` COUNT=0 → 切回 `100` COUNT=1 |
| 24 | 多数据源 — 从库路由 `@DS("slave")` | `slaveDataSourceRoutesToSecondH2`<br>`mdTenantDemo_httpUsesSlaveDsAppContext_andTenantHeader` | `MicroMysqlAdvancedFeaturesIT` | `SlaveProbeMapper.ping()` → `SELECT 1` 走 slave H2 |
| 25 | `SlaveProbeMapper` — `@DS` 注解路由 | `slaveDataSourceRoutesToSecondH2` | `MicroMysqlAdvancedFeaturesIT` | 直接 `assertEquals(1, slaveProbeMapper.ping())` |
| 26 | Liquibase — 4 个 changelog 顺序执行 | `contextLoads` | `MicroMysqlApplicationTests` | 建表后 `SELECT COUNT(*)` 不抛异常即成功 |
| 27 | DTO — `CreateMdSampleRequest` 字段绑定 | `dtosAndEntities_roundTripJson`<br>`mdSample_mapperThenServiceThenHttp` | `MicroMysqlDemoFullStackIT` | JSON 往返 + HTTP 入参解析 |
| 28 | DTO — `CreateMdFormRowRequest` / `UpdateMdFormRowRequest` | `dtosAndEntities_roundTripJson`<br>各 `mdFormRow` HTTP 用例 | `MicroMysqlDemoFullStackIT`<br>`MicroMysqlH2ControllerToDatabaseIT` | JSON 往返 + HTTP PUT |
| 29 | DTO — `MdFormRowResponse` 出参不含实体暴露 | 各 `mdFormRow` HTTP 用例 | `MicroMysqlDemoFullStackIT`<br>`MicroMysqlH2ControllerToDatabaseIT` | 响应类型为 `ResponseEntity<MdFormRowResponse>` |
| 30 | DTO — `CreateMdTenantDemoRequest` / `MdTenantDemoResponse` | `dtosAndEntities_roundTripJson`<br>各 `mdTenantDemo` HTTP 用例 | `MicroMysqlDemoFullStackIT`<br>`MicroMysqlAdvancedFeaturesIT` | JSON 往返 + HTTP POST 带租户头 |

---

## 3. 测试类清单

| 测试类 | Profile | 用例数 | 职责 |
|--------|---------|--------|------|
| `MicroMysqlApplicationTests` | `h2` | 1 | 上下文加载 + `md_sample` / `md_form_row` 表存在性 |
| `MicroMysqlH2ControllerToDatabaseIT` | `h2` | 4 | `MdSample` / `MdFormRow` HTTP 层 CRUD（含 DELETE 404、空 payload、list 数组） |
| `MicroMysqlDemoFullStackIT` | `h2` | 4 | 全链路：Mapper → Service → HTTP；全部 DTO / 实体 JSON 往返；`MdTenantDemo` 租户全链路 |
| `MicroMysqlAdvancedFeaturesIT` | `h2-advanced` | 5 | 多数据源（slave 路由）、多租户拦截条件、`remark` V4 列、`MdTenantDemoService`、租户 HTTP |

**合计：14 个测试用例，覆盖 30 个特性点。**

---

## 4. 未覆盖项（有意为之）

| 项 | 原因 |
|----|------|
| `MdSample.formJson` 业务语义校验 | 仅为 `String` 自由字段，业务规则由上游定义 |
| `MdFormRow.payload` JSON Schema 校验 | 由业务层自行约束，框架仅负责 TypeHandler 映射 |
| `MdTenantDemo` 的 UPDATE / DELETE HTTP 接口 | Demo 以 CRUD 为主，`MdFormRow` 已覆盖完整 UPDATE/DELETE 路径 |
| 悲观锁 / 乐观锁 | MyBatis-Plus 内置支持，非本 Demo 演示重点 |
| 事务传播行为 | `remark` 列写操作在单用例内完成，未跨多 Service 调用 |

---

## 5. 测试命令参考

```powershell
# 单独跑 demo 模块（含 *IT）
mvn test -pl jbm-examples/jbm-examples-mysql

# 带反应堆编译（首次或框架有变更时）
mvn test -pl jbm-examples/jbm-examples-mysql -am "-Dsurefire.failIfNoSpecifiedTests=false"

# 指定测试类
mvn test -pl jbm-examples/jbm-examples-mysql "-Dtest=MicroMysqlDemoFullStackIT,MicroMysqlAdvancedFeaturesIT"
```
