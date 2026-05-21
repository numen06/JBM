# Masterdata 相关 ORM 体系说明与子项目迁移指引

本文描述当前框架内 **Liquibase（结构版本化）+ MyBatis-Plus（日常数据访问）** 的分工与依赖落点；**应用分层与事务**见 **[CBSM-standard.md](./CBSM-standard.md)**；**JPA 不参与业务**，仅可在独立工具/一次性初始化场景使用（见 `jbm-framework-autoconfigure-jpa`）；**Flyway 已从框架移除**。**可运行联调与集成测试**集中在 **`jbm-examples/jbm-examples-mysql`**（与 `jbm-util` 同级，由 **`jbm-examples`** 聚合）；业务模块若需同款依赖组合可引用 **`com.jbm:jbm-framework-micro-mysql`**（**仅 POM 聚合传递依赖，不含演示代码**）。

---

## 1. 分工

| 层次 | 技术 | 职责 | 典型配置 |
|------|------|------|----------|
| 结构版本化（长期） | Liquibase | 表、索引、约束等以 changelog 版本管理 | 例如 `spring.liquibase.change-log=classpath:db/examples-mysql/db.changelog-master.yaml`（示例模块路径，避免与依赖 jar 中同名资源冲突） |
| 运行时 ORM | MyBatis-Plus | CRUD、分页、多租户插件、TypeHandler（如 JSON Map）等 | `mybatis-plus.*` 与 `jbm-framework-autoconfigure-mybatis` |
| 一次性结构对齐（可选、离线/独立模块） | Hibernate / JPA | 不再进入业务请求链路；仅用于受控环境下的建表对齐后导出 Liquibase 基线 | 独立引入 `jbm-framework-autoconfigure-jpa` 的 profile 或离线任务 |

**执行顺序**：应用启动时由 Spring Boot 执行 **Liquibase**，再使用 **MyBatis-Plus** 访问数据库。勿再与 Flyway 混用（Flyway 已删除）。

---

## 2. 依赖落点：Liquibase 与 MyBatis-Plus 同轨

**`org.liquibase:liquibase-core` 已声明在 `jbm-framework-autoconfigure-mybatis` 的 `pom.xml` 中。**

- 引入 **`jbm-framework-autoconfigure-mybatis`** 的应用，classpath 上即具备 Liquibase，在存在 `DataSource` 且配置 `spring.liquibase.change-log` 时由 **`LiquibaseAutoConfiguration`** 启用。
- **业务子项目不应再单独声明 `liquibase-core`**。
- 若仍需 **JPA 仅做初始化**：单独依赖 **`jbm-framework-autoconfigure-jpa`**（不再携带 Liquibase；与业务 MP 模块解耦）。

---

## 3. 子项目接入清单（迁移时逐项核对）

### 3.1 Maven

1. 日常业务：依赖 **`jbm-framework-autoconfigure-mybatis`**（已传递 **Liquibase**）。
2. 不在业务 `pom` 中重复添加 **`liquibase-core`**。
3. 需要 JPA 一次性工具时：按需增加 **`jbm-framework-autoconfigure-jpa`**（与 MP 分 profile，避免双 ORM 同时参与业务）。

### 3.2 配置（`application.yml` / `application-*.yml`）

1. **`spring.liquibase.change-log`**：指向本模块 `src/main/resources` 下的主 changelog。
2. **`spring.liquibase.enabled`**：生产环境建议 `true`。
3. **多自动配置并存**：若与框架自带 MVC 错误页等 Bean 重名，可配置 **`spring.main.allow-bean-definition-overriding`**（参考示例模块 `jbm-examples-mysql` 的 `application-h2.yml`）。

### 3.3 资源目录约定

- 建议路径：**`src/main/resources/db/changelog/`**（或业务专属子目录，如示例中的 **`db/examples-mysql/`**，保证 classpath 内 **`change-log` 路径全局唯一**）
  - `db.changelog-master.yaml`（或 `.xml`）只做 `include`。
  - 各版本变更放在 `changes/` 下，如 `V1__xxx.yaml`。

### 3.4 Flyway

**已移除**：`jbm-framework-autoconfigure-mybatis` 不再注册 Flyway、不再依赖 `flyway-core`。历史脚本目录 `classpath:db/migration` 若仍存在，不会被框架自动执行；请迁移为 Liquibase changelog。

### 3.5 本地构建

在仓库根 **`jbm`**（与 `jbm-framework`、`jbm-util` 同级）目录执行：

```text
mvn -pl jbm-examples/jbm-examples-mysql -am test
```

若在子目录单独执行 `mvn test`，请先在同一 reactor 下 **`install`/`compile`** 过 **`jbm-framework-autoconfigure-mybatis`**，避免本地仓库元数据过旧导致缺少 Liquibase。

---

## 4. 验证工程

### 4.0 公共依赖聚合（无演示代码）

- **路径**：`jbm-framework/jbm-framework-micro/jbm-framework-micro-mysql`
- **职责**：**仅**在 `pom.xml` 中聚合 `spring-boot-starter-web`、`jbm-framework-autoconfigure-mvc`、`jbm-framework-autoconfigure-mybatis`、可选 `dynamic-datasource`、H2（runtime）等；**不包含** `MicroMysqlApplication`、Controller、实体或 Liquibase 资源。集群等业务模块可依赖本 artifact 以统一版本与传递依赖。

### 4.0.1 示例与联调入口（主代码 + 集成测试）

- **路径**：`jbm-examples/jbm-examples-mysql`（父聚合 `jbm-examples` 与 `jbm-util` 同级）
- **职责**：`src/main/java` 下为 **`MicroMysqlApplication`**、Controller/Service/Mapper、实体、租户演示、`src/main/resources` 下 **`application-h2.yml` / Liquibase changelog**；`src/test/java` 为 H2 端到端 **`@SpringBootTest`**。依赖 **`com.jbm:jbm-framework-micro-mysql`** 获取上述框架栈。

### 4.1 H2 配置（`h2` profile）

- **`application.yml`**：`spring.profiles.active: h2`，并保留 `mybatis-plus` 公共项。
- **`application-h2.yml`**：H2 JDBC URL、`DataSource`、`liquibase`、`spring.h2.console`、`spring.main.allow-bean-definition-overriding`。
- 测试类 **`@ActiveProfiles("h2")`**。

### 4.2 代码分层（Controller → Service → Mapper）

| 链路 | Controller | Service | Mapper | 表 / changelog |
|------|------------|---------|--------|----------------|
| MP 主表 | `MdSampleController`（`jbm-examples-mysql` `src/main/java`）`/api/h2/mp/samples` | `MdSampleService` | `MdSampleMapper` | `md_sample`（`db/examples-mysql/changes/V2__md_sample.yaml`） |
| MP JSON | `MdFormRowController`（同模块，见下） | `MdFormRowService` | `MdFormRowMapper` | `md_form_row`（`db/examples-mysql/changes/V1__md_form_row.yaml` + `JacksonTypeHandler`） |

**`md_form_row`（动态 `payload` JSON）REST 约定**：基路径 `/api/h2/mp/form-rows`；`GET` 列表、`GET /{id}`、`POST`（`CreateMdFormRowRequest`）、`PUT /{id}` 全量替换 `payload`（`UpdateMdFormRowRequest`）、`DELETE /{id}`；响应体为 `MdFormRowResponse`。不存在时返回 **404**（`ResponseEntity`），不经由 `ResponseStatusException` 以免被全局异常处理成 500。Controller **不继承** `MasterDataCollection`，不使用 `validatorMasterData` 等通用请求体反序列化。

### 4.3 自动化测试

- **所在模块**：`jbm-examples/jbm-examples-mysql/src/test/java/...`；测试 classpath 使用 **`logback-test.xml`**。
- **表存在性**：`MicroMysqlApplicationTests`
- **HTTP 端到端**：`MicroMysqlH2ControllerToDatabaseIT`（`TestRestTemplate` + `WebEnvironment.RANDOM_PORT`）
- **多数据源 / 多租户 / 字段演进**：`MicroMysqlAdvancedFeaturesIT`（`spring.profiles.active=h2-advanced`：主从双 H2、`@DS("slave")` 探测、`DemoTenantLineHandler` 租户隔离、Liquibase **V4** 为 `md_tenant_demo` 增加 **`remark`** 列并写入）

日志：示例测试仅使用 **`logback-test.xml`**。

### 4.4 入口与包

- 入口：`com.jbm.micro.mysql.MicroMysqlApplication`（位于 **`jbm-examples-mysql`** 的 `src/main/java`，仅 **`@MapperScan`**）
- 实体与 Mapper：同上模块 `com.jbm.micro.mysql.mp`、`com.jbm.micro.mysql.mapper`

---

## 5. 后续全仓迁移建议顺序（各子项目执行）

1. **锁定结构真源**：统一 **Liquibase**。
2. **从现有库导出基线**：`generateChangeLog` 得到 `V0` 基线纳入 Git；后续仅增量 changelog。
3. **业务仅保留 MyBatis-Plus**：Controller/Service 不经过 JPA。
4. **移除 Flyway 残留**：删除各模块 `db/migration` 依赖与配置；清理文档中对 Flyway 的引用。
5. **回归**：空库启动、Liquibase 全量执行、核心接口冒烟。

---

## 6. 代码生成器与文档

- Liquibase **导出命令** 的补充说明见 **`GenerateMasterData`** 类注释。
- **实体基类老体系 → `MasterDataEntity` 单基类**：见 **[Masterdata-ORM基类继承迁移说明.md](Masterdata-ORM基类继承迁移说明.md)**（继承对照、`exist=false` 规则、Service/Controller 清单）。
- 可与 **`优化.md`**、**Masterdata 模块架构升级计划**（`.cursor/plans/`）对照；**以本文与当前 `pom` 为准**执行迁移。

如有单个子项目的 `pom` / 配置与本文冲突，以子项目 README 或该域负责人更新为准，并建议回链本文。
