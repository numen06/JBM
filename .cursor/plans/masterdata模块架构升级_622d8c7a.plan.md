---
name: Masterdata模块架构升级
overview: 重构 jbm-framework-data-masterdata 模块，消除Controller深度继承链，重新设计DAO/Service/Business三层架构，修复事务自调用失效问题，JPA仅用于首次初始化数据库生成Liquibase changelog后移除，日常完全使用Liquibase+MyBatis-Plus，升级代码生成器从"空壳+继承"模式改为"完整代码生成"模式，支持多租户和自定义表单JSON扩展。最终在 jbm-framework-micro-mysql 项目中完成最小化完整流程验证。
todos:
  - id: phase0-jpa-initialize
    content: JPA 仅用于首次初始化：在 micro-mysql 项目中保留 JPA 依赖和 ddl-auto=update，启动一次生成完整数据库结构
    status: completed
  - id: phase0-export-liquibase-changelog
    content: 从 JPA 初始化的数据库导出 Liquibase changelog（V0），包含所有表/索引/约束定义
    status: completed
  - id: phase0-remove-jpa
    content: 移除 JPA 依赖和注解：删除 jbm-framework-autoconfigure-jpa 模块，清理 pom.xml 和实体类中的 JPA 注解
    status: cancelled
  - id: phase0-integrate-liquibase
    content: 引入 Liquibase 最小化依赖：添加 spring-boot-starter-liquibase，配置 changelog 路径，接管数据库版本管理
    status: completed
  - id: phase0-tenant-config
    content: 配置多租户拦截器：TenantLineInnerInterceptor 自动拼接 tenant_id 条件，支持忽略公共表
    status: completed
  - id: phase0-json-form-support
    content: 自定义表单支持：实体中增加 @TableField(typeHandler = JacksonTypeHandler.class) Map 字段，MySQL 8 JSON 存储
    status: completed
  - id: phase1-delete-old-controller
    content: 删除旧的 Controller Collection 基类和接口（BaseCollection、MasterDataCollection、MasterDataTreeCollection、MultiPlatformCollection、IMasterDataController 等）
    status: cancelled
  - id: phase1-new-base-controller
    content: 新建轻量 BaseController 工具基类（仅提供响应封装、校验辅助，不提供端点实现）
    status: completed
  - id: phase2-refactor-service-interfaces
    content: 精简 Service 层：删除 IMasterDataService、IMultiPlatformService，精简 IBaseService、IMasterDataTreeService
    status: cancelled
  - id: phase2-refactor-service-impls
    content: 重构 ServiceImpl 基类：删除 MasterDataServiceImpl/MasterDataTreeServiceImpl/MultiPlatformServiceImpl，精简 BaseServiceImpl
    status: cancelled
  - id: phase3-create-business-layer
    content: 新建 BaseBusiness 和 TreeBusiness 基类，所有写方法标注 @Transactional(rollbackFor = Exception.class)
    status: completed
  - id: phase3-delete-old-business-transaction
    content: 删除旧的 Business 空壳（IPlatformBusiness、PlatformBusinessImpl）和旧事务 AOP 配置（TransactionAdviceConfig、EnableTransactionAdviceManagement）
    status: completed
  - id: phase4-update-templates
    content: 升级所有 .btl 模板：controller 模板显式声明端点，service 模板继承 IService，business 模板包含完整 CRUD + @Transactional
    status: completed
  - id: phase5-update-generators
    content: 升级代码生成器类：GenerateControllerCode/GenerateServiceCode/GenerateServiceImplCode/GenerateBusinessCode/GenerateBusinessImplCode，删除继承映射逻辑
    status: completed
  - id: phase5-update-generate-masterdata
    content: 更新 GenerateMasterData.java：调整生成顺序和模块控制，简化 SuperClass 解析逻辑，增加 Liquibase changelog 生成
    status: completed
  - id: phase6-micro-mysql-test
    content: 在 jbm-framework-micro-mysql 项目中完成完整流程验证：JPA初始化->Liquibase接管->代码生成->CRUD验证->事务验证
    status: pending
isProject: false
---

# Masterdata 模块架构升级计划

> 整合 [优化.md](优化.md) 架构蓝图：**JPA 般的开发体验 + MyBatis 的绝对掌控力 + Liquibase 守住生产安全线**

## 问题分析

### 当前架构痛点

1. **Controller 深度继承链**：`Controller -> MasterDataCollection -> BaseCollection`（3层），所有CRUD端点隐式继承，难以理解和调试
2. **Service 方法膨胀**：`IMasterDataService` 定义 30+ 方法，`MasterDataServiceImpl` 实现大量与 MyBatis-Plus `IService` 重叠的方法
3. **事务自调用失效**：`MasterDataServiceImpl` 中 `this.deleteById(id)`、`this.saveOrUpdateBatch()` 等自调用绕过 Spring AOP 代理，`@Transactional` 不生效
4. **Business 层形同虚设**：`IPlatformBusiness` 和 `PlatformBusinessImpl` 是空壳，没有实际作用
5. **代码生成器产空壳**：生成的 Controller/Service/Mapper 全是空类，所有逻辑在框架基类中
6. **JPA 与 MyBatis-Plus 双 ORM 并存**：所有实体类同时标注 JPA（@Entity、@Id、@Column）和 MyBatis-Plus（@TableName、@TableId、@TableField）注解，引入 Hibernate 仅用于 ddl-auto=update 自动建表，增加约 20MB 依赖体积和启动时间
7. **JPA 自动建表不可控**：`ddl-auto=update` 只能添加新列，不能删除/修改已有列，索引和约束管理不可控，生产环境存在风险

---

## 核心架构理念

```
JPA 一次性初始化  -> 快速生成完整数据库结构
     |
Liquibase 接管    -> 版本化 changelog 管理所有后续变更
     |
MyBatis-Plus      -> 日常 CRUD 和数据操作
```

## Phase 0: JPA 初始化 -> Liquibase 接管 -> 移除 JPA -> 微项目验证

### 0.1 JPA 仅用于首次初始化（在 jbm-framework-micro-mysql 中执行）

**目的**：利用 JPA 的 `ddl-auto=update` 快速生成完整的初始数据库结构。

**步骤**：
1. 在 `jbm-framework-micro-mysql` 项目中确保 JPA 依赖有效：`spring.jpa.hibernate.ddl-auto=update`
2. 创建测试实体类（包含继承链的完整示例）
3. 启动项目，让 Hibernate 根据所有 `@Entity` 注解创建完整的表结构
4. 验证数据库包含所有需要的表、索引、约束
5. **仅此一次**，之后 JPA 不再参与数据库管理

### 0.2 导出 Liquibase Changelog

**从初始化后的数据库导出完整结构**：

```bash
# 使用 liquibase generate-change-log 导出完整数据库结构为 changelog
liquibase \
  --driver=com.mysql.cj.jdbc.Driver \
  --url="jdbc:mysql://localhost:3306/your_db" \
  --username=root \
  --password=your_password \
  --changeLogFile=db/changelog/V0__initial_schema.xml \
  generateChangeLog
```

**或使用 Maven 插件**：
```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <configuration>
        <driver>com.mysql.cj.jdbc.Driver</driver>
        <url>jdbc:mysql://localhost:3306/your_db</url>
        <username>root</username>
        <password>your_password</password>
        <changeLogFile>src/main/resources/db/changelog/V0__initial_schema.xml</changeLogFile>
    </configuration>
</plugin>
```

```bash
mvn liquibase:generateChangeLog
```

**生成的 changelog 格式**（XML 示例）：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="V0-1" author="system">
        <createTable tableName="base_user">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="user_name" type="VARCHAR(128)"/>
            <column name="create_time" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="update_time" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"/>
        </createTable>
    </changeSet>

    <changeSet id="V0-2" author="system">
        <createIndex indexName="uk_api_key" tableName="base_app" unique="true">
            <column name="api_key"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

### 0.3 移除 JPA 依赖和注解

**删除 JPA 相关模块**：

| 模块/文件 | 操作 |
|-----------|------|
| `jbm-framework-autoconfigure-jpa/` | **删除整个模块** |
| `jbm-framework-autoconfigure-jpa-h2/` | **删除整个模块** |
| `jbm-framework-dependencies/pom.xml` | 删除 `spring-boot-starter-data-jpa` 依赖管理 |
| `jbm-cluster-api-basic/pom.xml` | 删除 `spring-boot-starter-data-jpa` 依赖 |
| `jbm-framework-micro-mysql/pom.xml` | 删除 `jbm-framework-autoconfigure-jpa` 依赖 |
| `jpa.properties` | **删除** |

**清理实体类中的 JPA 注解**（约 48 个类）：

| JPA 注解 | 处理方式 |
|----------|----------|
| `@Entity` | **删除** |
| `@MappedSuperclass` | **删除** |
| `@Table(name=..., indexes=...)` | 表名保留到 `@TableName` |
| `@Id` | **删除**（已有 `@TableId`） |
| `@GeneratedValue` | **删除** |
| `@Column(name=...)` | 改为 `@TableField("column_name")` |
| `@Column(columnDefinition=...)` | 信息记录到 Liquibase changelog |
| `@Transient` | 改为 `@TableField(exist = false)` |
| `@Enumerated` | **删除**（MyBatis-Plus 默认字符串） |
| `@Lob` | **删除**（在 changelog 中定义类型） |
| `@EntityScan` | **从启动类删除** |

### 0.4 引入 Liquibase 最小化依赖

**添加依赖**（仅 `spring-boot-starter-liquibase`）：

```xml
<!-- jbm-framework-dependencies/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
    <version>${spring-boot.version}</version>
</dependency>

<!-- 各业务模块 pom.xml -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

**最小化配置**：

```yaml
# application.yml（所有环境通用基础配置）
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    # 不自动清理数据库
    clean-disabled: true
    # 首次执行时标记基线版本，不执行已有变更集（适合已有数据的库）
    baseline-on-migrate: true
    baseline-version: 0
```

**Changelog 目录结构**：
```
src/main/resources/db/changelog/
  db.changelog-master.xml          # 主 changelog，引入所有子 changelog
  V0__initial_schema.xml           # 初始完整表结构（从 JPA 导出）
  V1__add_base_dic_table.xml       # 增量变更
  V2__alter_base_user_add_email.xml
  ...
```

**db.changelog-master.xml**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- 初始完整表结构 -->
    <include file="db/changelog/V0__initial_schema.xml"/>

    <!-- 后续增量变更 -->
    <includeAll path="db/changelog/changes/" relativeToChangelogFile="true"/>
</databaseChangeLog>
```

### 0.5 日常开发工作流（Liquibase 管理）

```
1. 新增实体/字段
   -> 实体类中加字段（仅 MyBatis-Plus 注解）
   -> 编写 Liquibase changelog XML 描述变更
   -> 放入 db/changelog/changes/ 目录

2. 启动应用
   -> Liquibase 自动比对版本，执行未应用的 changelog
   -> 数据库结构同步

3. 生产部署
   -> 应用启动时 Liquibase 自动执行未应用的 changelog
   -> 版本记录清晰可追溯
```

**增量 changelog 示例**（`changes/V1__add_base_dic_table.xml`）：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="V1-1" author="developer">
        <createTable tableName="base_dic">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(128)"/>
            <column name="code" type="VARCHAR(64)"/>
            <column name="parent_id" type="BIGINT"/>
            <column name="level" type="INT"/>
            <column name="create_time" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP"/>
            <column name="update_time" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"/>
        </createTable>
    </changeSet>

    <changeSet id="V1-2" author="developer">
        <createIndex indexName="idx_base_dic_parent_id" tableName="base_dic">
            <column name="parent_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

### 0.6 代码生成器扩展：Liquibase changelog 生成

新增 `GenerateLiquibaseChangelog.java` 生成器：
- 扫描实体类字段（含继承链）
- 根据字段类型生成对应的 Liquibase `<changeSet>` XML
- 自动递增 changeSet id
- 输出到 `db/changelog/changes/V{version}__create_{table_name}.xml`

修改 `GenerateMasterData.java`：
- `enableChangelog` 默认值 `true`
- 生成顺序：Mapper -> Service -> ServiceImpl -> Business -> BusinessImpl -> Changelog -> Controller

### 0.7 多租户拦截器配置

通过 MyBatis-Plus 的 `MybatisPlusInterceptor` 配置 `TenantLineInnerInterceptor`：

```java
@Configuration
public class TenantConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContextHolder.getTenantId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return Arrays.asList("sys_dict", "sys_config", "sys_menu").contains(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

### 0.8 自定义表单 JSON 扩展

```java
@TableName("sys_custom_form")
public class CustomForm extends MasterDataIdEntity {

    @TableField("form_name")
    private String formName;

    // MySQL 8 JSON 类型，JacksonTypeHandler 自动映射
    @TableField(value = "custom_fields", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> customFields;
}
```

查询：`wrapper.apply("custom_fields->>'$.customerLevel' = {0}", "A");`

高频字段优化（Liquibase 添加虚拟列索引）：
```xml
<changeSet id="V2-1" author="developer">
    <sql>
        ALTER TABLE sys_custom_form
        ADD COLUMN level_virtual VARCHAR(50)
        GENERATED ALWAYS AS (custom_fields->>'$.customerLevel') VIRTUAL;
    </sql>
</changeSet>
<changeSet id="V2-2" author="developer">
    <createIndex indexName="idx_level" tableName="sys_custom_form">
        <column name="level_virtual"/>
    </createIndex>
</changeSet>
```

---

## Phase 1-5: 新架构设计（DAO/Service/Business/Controller 四层）

### 分层原则

```
Controller 层 -> 显式声明端点，委托给 Business 层
     |
Business 层    -> 事务边界 + 业务编排 + CRUD 实现（通过 Service 或直接通过 Mapper）
     |
Service 层    -> 薄层，仅封装自定义查询/转换逻辑
     |
Mapper (DAO) -> MyBatis-Plus SuperMapper，保持不变
```

### 各层职责

| 层级 | 职责 | 事务 |
|------|------|------|
| **Controller** | 接收请求、参数校验、委托调用 Business、返回响应 | 无 |
| **Business** | 事务边界、业务编排、多Service协调、CRUD基础实现 | `@Transactional` 标注在 Business 方法上 |
| **Service** | 自定义查询逻辑、数据转换、薄封装 | 无事务（由 Business 层统一管控） |
| **Mapper** | 数据库访问 | 保持不变 |

### 事务问题修复

**根本原因**：`EnableTransactionAdviceManagement` 使用 `AdviceMode.PROXY`，同类内 `this.method()` 调用不经过代理。

**修复方案**：
- 事务统一放在 **Business 层** 的方法上，使用 `@Transactional(rollbackFor = Exception.class)`
- Service 层不再声明 `@Transactional`，避免事务嵌套混乱
- 业务编排（如 `service.save()` 后调 `otherService.query()`）统一在 Business 层的一个带事务的方法内完成
- 移除 `TransactionAdviceConfig` 的 AOP 自动事务（对同类自调用同样无效），改为显式 `@Transactional`

---

## Phase 1: 重新设计 Controller 层（去除继承）

### 1.1 删除旧的 Collection 基类和接口

| 文件 | 路径 | 操作 |
|------|------|------|
| `BaseCollection.java` | `jbm-framework-autoconfigure-mvc/.../web/` | **删除** |
| `MasterDataCollection.java` | 同上 | **删除** |
| `MasterDataTreeCollection.java` | 同上 | **删除** |
| `MultiPlatformCollection.java` | 同上 | **删除** |
| `IMasterDataController.java` | `jbm-framework-data-masterdata/.../controller/` | **删除** |
| `IMasterDataTreeController.java` | 同上 | **删除** |
| `IMultiPlatformController.java` | 同上 | **删除** |
| `IMultiPlatformTreeController.java` | 同上 | **删除** |

### 1.2 创建新的 BaseController（轻量工具基类）

新建 `BaseController`：
- 不提供任何 REST 端点实现
- 仅提供通用工具方法：统一响应封装、参数校验辅助、国际化消息辅助
- 可选继承，Controller 也可以直接不继承

```java
public abstract class BaseController {
    @Autowired protected MessageSource messageSource;

    protected <T> ResultBody<T> success(T data) { ... }
    protected <T> ResultBody<T> fail(String msg) { ... }
    protected void validate(Entity entity) { ... }
}
```

### 1.3 Controller 模板改为显式声明

新 Controller 模板生成的代码：
```java
@Api(tags = "区域管理")
@RestController
@RequestMapping("/baseArea")
public class BaseAreaController extends BaseController {

    @Autowired private BaseAreaBusiness baseAreaBusiness;

    @PostMapping("/pageList")
    public ResultBody<Page<BaseArea>> pageList(@RequestBody PageRequestBody req) {
        return success(baseAreaBusiness.pageList(req));
    }

    @PostMapping("/list")
    public ResultBody<List<BaseArea>> list(@RequestBody MasterDataRequsetBody req) {
        return success(baseAreaBusiness.list(req));
    }

    @PostMapping("/model")
    public ResultBody<BaseArea> model(@RequestBody EntityRequestForm<BaseArea> req) {
        return success(baseAreaBusiness.selectEntity(req));
    }

    @PostMapping("/save")
    public ResultBody<BaseArea> save(@RequestBody EntityRequestForm<BaseArea> req) {
        return success(baseAreaBusiness.saveEntity(req.getData()));
    }

    @PostMapping("/delete")
    public ResultBody<Boolean> delete(@RequestBody IdsForm form) {
        return success(baseAreaBusiness.deleteByIds(form.getIds()));
    }

    // 自定义业务方法直接加在这里
}
```

---

## Phase 2: 重新设计 Service 层（精简）

### 2.1 删除/精简 Service 接口

| 文件 | 操作 |
|------|------|
| `IBaseService.java` | **精简**：只保留 `pageList`、`getEntityMap` 等 MyBatis-Plus 未提供的方法 |
| `IMasterDataService.java` | **删除**：方法要么已存在于 `IService`，要么移入 Business 层 |
| `IMasterDataTreeService.java` | **精简**：只保留树形特有方法签名（`selectRootListById`、`selectChildNodesById` 等） |
| `IMultiPlatformService.java` | **删除**：空接口无意义 |
| `IBaseSqlService.java` | **删除** |

新的 Service 接口范式：
```java
public interface XxxService extends IService<Xxx> {
    // 仅声明自定义查询方法，CRUD 全部来自 IService
    Page<Xxx> pageCustom(CriteriaQueryWrapper<Xxx> wrapper);
    // ... 其他自定义方法
}
```

### 2.2 重构 ServiceImpl 基类

| 文件 | 操作 |
|------|------|
| `BaseServiceImpl.java` | **精简**：移除与 MyBatis-Plus `ServiceImpl` 重叠的方法，保留 `pageList`（XML 外键连接）等独有功能 |
| `MasterDataServiceImpl.java` | **删除**：所有 `@Transactional` 标注的方法移到 Business 层；其余重复方法删除 |
| `MasterDataTreeServiceImpl.java` | **删除**：树形查询方法移入 TreeBusiness |
| `MultiPlatformServiceImpl.java` | **删除**：空壳类 |

新的 ServiceImpl 范式：
```java
@Service
public class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService {
    // 仅实现自定义查询方法
    // 无 @Transactional 注解
}
```

---

## Phase 3: 新增 Business 层（事务边界 + 业务编排）

### 3.1 创建 Business 基类

新建 `BaseBusiness<Entity, Service>` 抽象类：
- 注入 `Service` 和 `Mapper`
- 提供基础 CRUD 方法（原 `MasterDataServiceImpl` 中的方法迁移到这里）
- 所有写方法标注 `@Transactional(rollbackFor = Exception.class)`
- 支持自定义业务方法扩展

```java
public abstract class BaseBusiness<Entity extends MasterDataIdEntity, Service extends IService<Entity>> {
    @Autowired protected Service service;
    @Autowired protected SuperMapper<Entity> mapper;

    @Transactional(rollbackFor = Exception.class)
    public Entity saveEntity(Entity entity) { ... }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Serializable id) { ... }

    // 其他 CRUD 方法...
}
```

新建 `TreeBusiness<Entity, Service>` 扩展树形操作：
```java
public abstract class TreeBusiness<Entity extends MasterDataTreeEntity, Service extends IService<Entity>>
    extends BaseBusiness<Entity, Service> {

    public List<Entity> selectRootList() { ... }
    public List<Entity> selectTree() { ... }
}
```

### 3.2 删除旧的 Business 空壳和旧事务配置

| 文件 | 操作 |
|------|------|
| `IPlatformBusiness.java` | **删除** |
| `PlatformBusinessImpl.java` | **删除** |
| `TransactionAdviceConfig.java` | **删除**（AOP 事务对同类自调用无效） |
| `EnableTransactionAdviceManagement.java` | **删除** |

---

## Phase 4: 升级代码生成器模板

### 4.1 修改模板文件

| 模板文件 | 修改内容 |
|----------|----------|
| `controller.btl` | 改为显式声明所有 CRUD 端点 + `@Autowired` 注入 Business，不再 `extends Collection` |
| `service.btl` | 改为 `extends IService<Entity>`，仅保留自定义方法声明位置 |
| `serviceImpl.btl` | 改为 `extends ServiceImpl<Mapper, Entity>`，无 `@Transactional` |
| `mapper.btl` | **保持不变** |
| `business.btl` | 改为接口声明，继承 `BaseBusiness` |
| `businessImpl.btl` | 改为实现类，包含 CRUD 方法实现 + `@Transactional` |
| `mapperXml.btl` | **保持不变** |

### 4.2 修改生成器逻辑

| 生成器类 | 修改内容 |
|----------|----------|
| `GenerateControllerCode.java` | 删除 `getSuperClass()` 继承映射，改为生成完整 Controller 代码 |
| `GenerateServiceCode.java` | 修改 `extClass` 为 `IService`，不再根据实体类型选择不同父接口 |
| `GenerateServiceImplCode.java` | 修改 `extClass` 为 `ServiceImpl`，简化逻辑 |
| `GenerateBusinessCode.java` | 修改：生成 Business 接口，继承 `BaseBusiness` |
| `GenerateBusinessImplCode.java` | 修改：生成包含完整 CRUD 实现的 BusinessImpl + `@Transactional` |

---

## Phase 6: jbm-framework-micro-mysql 最小化测试验证

### 测试项目现状

`jbm-framework-micro-mysql` 项目当前状态：
- **路径**: `jbm-framework/jbm-framework-micro/jbm-framework-micro-mysql/`
- **当前依赖**: `spring-boot-starter-web`, `jbm-framework-autoconfigure-mvc`, `jbm-framework-autoconfigure-jpa`, `jbm-framework-autoconfigure-mybatis`
- **当前代码**: 仅有一个空的 `SpringBootDemo` 启动类和 `SpringBootService`

### 完整测试流程

#### 步骤 1: 准备测试环境

```
1. 修改 jbm-framework-micro-mysql/pom.xml
   -> 确保包含 masterdata 依赖
   -> 包含 JPA（初始化用）、MyBatis-Plus、Liquibase 依赖

2. 配置 application.yml
   -> 数据库连接（MySQL 8）
   -> JPA ddl-auto=update（仅初始化用）
   -> Liquibase 配置
   -> MyBatis-Plus 配置
```

#### 步骤 2: JPA 初始化数据库

```
1. 创建测试实体类（示例）
   -> TestEntity extends MasterDataIdEntity（简单CRUD）
   -> TestTreeEntity extends MasterDataTreeEntity（树形结构）
   -> TestCustomForm（自定义表单 JSON 扩展）

2. 启动应用
   -> JPA 根据 @Entity 注解自动建表
   -> 验证数据库结构完整

3. 导出 Liquibase changelog
   -> mvn liquibase:generateChangeLog
   -> 生成 V0__initial_schema.xml
   -> 验证 changelog 包含所有表/索引/约束
```

#### 步骤 3: 移除 JPA

```
1. 删除 pom.xml 中 jbm-framework-autoconfigure-jpa 依赖
2. 删除实体类中的 JPA 注解
   -> 批量清理 @Entity, @MappedSuperclass, @Id, @GeneratedValue, @Column
   -> 保留 MyBatis-Plus 注解
3. 删除启动类中的 @EntityScan（如有）

4. 启动应用
   -> Liquibase 应正常工作（baseline-on-migrate=true 跳过已有结构）
   -> JPA 不再参与数据库管理
```

#### 步骤 4: 代码生成器验证

```
1. 配置代码生成器扫描路径
2. 运行 GenerateMasterData 扫描测试实体
3. 验证生成结果：
   -> Mapper 接口（extends SuperMapper）
   -> Service 接口（extends IService）+ ServiceImpl
   -> Business 接口（extends BaseBusiness）+ BusinessImpl（@Transactional）
   -> Controller（显式声明端点，注入 Business）
   -> Liquibase changelog XML（新增表的 changeSet）
```

#### 步骤 5: CRUD 功能验证

```
1. 启动应用，Liquibase 执行 changelog 建表
2. 测试 Controller 端点：
   POST /testEntity/pageList  -> 分页查询
   POST /testEntity/list      -> 列表查询
   POST /testEntity/model     -> 单个查询
   POST /testEntity/save      -> 保存
   POST /testEntity/delete    -> 删除
   POST /testEntity/saveBatch -> 批量保存

3. 验证 Business 层事务：
   -> 在 BusinessImpl 中编写包含多个 Service 调用的方法
   -> 验证 @Transactional 生效（异常时回滚）
   -> 验证自调用事务生效（同类内方法调用）

4. 验证树形结构（如使用 TestTreeEntity）：
   POST /testTree/root        -> 根节点
   POST /testTree/tree        -> 树结构
```

#### 步骤 6: 增量变更验证

```
1. 给 TestEntity 增加字段
2. 编写增量 changelog（V1__add_xxx_column.xml）
3. 启动应用
   -> Liquibase 自动执行增量 changelog
   -> 验证新列已添加到数据库

4. 验证新字段可正常读写
```

#### 步骤 7: 多租户验证（可选）

```
1. 配置 TenantLineInnerInterceptor
2. 设置 TenantContextHolder 模拟不同租户
3. 验证查询自动拼接 tenant_id 条件
4. 验证公共表（如 sys_dict）不被添加 tenant_id 条件
```

#### 步骤 8: 自定义表单 JSON 验证（可选）

```
1. 使用 TestCustomForm 实体
2. 测试 JSON 字段读写
3. 测试 MySQL 8 JSON 查询（wrapper.apply）
4. 验证 JacksonTypeHandler 正确映射
```

### 验证成功标准

| 验证项 | 标准 |
|--------|------|
| JPA 初始化 | 启动后数据库包含完整表结构 |
| Liquibase 导出 | changelog 包含所有表/索引/约束 |
| JPA 移除 | 删除依赖和注解后应用正常启动 |
| Liquibase 接管 | 启动时自动执行 changelog |
| 代码生成 | 生成完整的 Mapper/Service/Business/Controller/Changelog |
| CRUD 功能 | 所有端点正常返回数据 |
| 事务管理 | @Transactional 回滚生效，自调用不失效 |
| 增量变更 | 新增 changelog 自动执行，字段变更可读写 |

---

## 迁移影响和兼容性

### 对现有业务代码的影响

| 层级 | 影响 | 迁移策略 |
|------|------|----------|
| Entity | **高影响** | 删除 JPA 注解，批量脚本处理 |
| Mapper | 无影响 | 保持不变 |
| Service 接口 | 中影响 | `IMasterDataService` 删除后，现有接口需改为 `extends IService` |
| ServiceImpl | 中影响 | `extends MasterDataServiceImpl` 改为 `extends ServiceImpl`，自定义方法保留 |
| Controller | **高影响** | `extends MasterDataCollection` 需改为显式声明端点（可自动生成） |
| Business | 新增 | 旧 Business 为空壳，需生成新的 Business/BusinessImpl |
| DDL | 新增 | 首次从 JPA 导出 Liquibase changelog |

### 迁移辅助

1. 编写 `JpaAnnotationCleaner.java` 工具类，批量扫描和清理 JPA 注解
2. 在框架升级完成后，提供一个迁移脚本，扫描 jbm-cluster 中的旧 Controller/Service，自动生成新格式代码
3. 首次 DDL 脚本：通过 `liquibase generateChangeLog` 从 JPA 初始化的数据库导出

---

## 文件变更清单

### 删除的文件

| 文件路径 | 说明 |
|----------|------|
| `jbm-framework-autoconfigure-jpa/` | JPA 自动配置模块（整个目录） |
| `jbm-framework-autoconfigure-jpa-h2/` | JPA H2 模块（整个目录） |
| `.../web/BaseCollection.java` | 空壳基类 |
| `.../web/MasterDataCollection.java` | Controller 继承基类 |
| `.../web/MasterDataTreeCollection.java` | 树形 Controller 基类 |
| `.../web/MultiPlatformCollection.java` | 多平台 Controller 基类 |
| `.../controller/IMasterDataController.java` | Controller 接口 |
| `.../controller/IMasterDataTreeController.java` | 树形 Controller 接口 |
| `.../controller/IMultiPlatformController.java` | 多平台 Controller 接口 |
| `.../service/IMasterDataService.java` | 膨胀的 Service 接口 |
| `.../service/IMultiPlatformService.java` | 空 Service 接口 |
| `.../service/IBaseSqlService.java` | SQL Service 接口 |
| `.../mybatis/MasterDataServiceImpl.java` | 带事务的 ServiceImpl |
| `.../mybatis/MasterDataTreeServiceImpl.java` | 树形 ServiceImpl |
| `.../mybatis/MultiPlatformServiceImpl.java` | 空 ServiceImpl |
| `.../business/IPlatformBusiness.java` | 空 Business 接口 |
| `.../business/PlatformBusinessImpl.java` | 空 Business 实现 |
| `.../transaction/TransactionAdviceConfig.java` | AOP 事务配置 |
| `.../transaction/EnableTransactionAdviceManagement.java` | 事务管理注解 |

### 新建的文件

| 文件路径 | 说明 |
|----------|------|
| `.../business/BaseBusiness.java` | 新 Business 基类（事务边界） |
| `.../business/TreeBusiness.java` | 树形 Business 基类 |
| `.../web/BaseController.java` | 轻量 Controller 工具基类 |
| `.../tenant/TenantConfiguration.java` | 多租户拦截器配置 |
| `.../code/generate/GenerateLiquibaseChangelog.java` | Liquibase changelog 代码生成器 |
| `db/changelog/db.changelog-master.xml` | Liquibase 主 changelog |
| `db/changelog/V0__initial_schema.xml` | 初始完整表结构（从 JPA 导出） |

### 修改的文件

| 文件路径 | 说明 |
|----------|------|
| `jbm-framework-micro-mysql/pom.xml` | 添加 Liquibase 依赖，移除 JPA 依赖 |
| `.../usage/entity/MasterDataEntity.java` | 删除 `@MappedSuperclass` |
| `.../usage/entity/MasterDataIdEntity.java` | 删除 `@Id`, `@GeneratedValue` |
| `.../usage/entity/MasterDataTreeEntity.java` | 删除 `@MappedSuperclass` |
| `.../usage/entity/MultiPlatformEntity.java` | 删除 `@MappedSuperclass` |
| `.../usage/entity/MultiPlatformIdEntity.java` | 删除 `@Id`, `@GeneratedValue`, `@MappedSuperclass` |
| `.../service/IBaseService.java` | 精简方法 |
| `.../service/IMasterDataTreeService.java` | 精简方法 |
| `.../mybatis/BaseServiceImpl.java` | 精简方法 |
| `.../code/btl/controller.btl` | 改为显式声明端点 |
| `.../code/btl/service.btl` | 改为 `IService` 继承 |
| `.../code/btl/serviceImpl.btl` | 改为 `ServiceImpl` 继承 |
| `.../code/btl/business.btl` | 改为 Business 接口模板 |
| `.../code/btl/businessImpl.btl` | 改为完整 Business 实现模板 |
| `.../code/GenerateControllerCode.java` | 删除继承映射逻辑 |
| `.../code/GenerateServiceCode.java` | 简化父类解析逻辑 |
| `.../code/GenerateServiceImplCode.java` | 简化父类解析逻辑 |
| `.../code/GenerateBusinessCode.java` | 调整生成逻辑 |
| `.../code/GenerateBusinessImplCode.java` | 调整生成逻辑 |
| `.../code/GenerateMasterData.java` | 调整生成顺序，增加 Liquibase changelog 生成 |
| jbm-cluster 中约 48 个实体类 | 删除 JPA 注解 |
| `mybatis-plus.properties` | 添加 Liquibase 配置 |

---

## 架构对比图

### 当前架构（改造前）

```mermaid
graph TD
    subgraph Controller层
        Ctrl[生成的Controller] -->|继承| MDC[MasterDataCollection]
        MDC -->|继承| BC[BaseCollection]
        MDC -->|实现| IMDC[IMasterDataController]
    end

    subgraph Service层
        Svc[生成的ServiceImpl] -->|继承| MDSI[MasterDataServiceImpl @Transactional]
        MDSI -->|继承| BSI[BaseServiceImpl]
        BSI -->|继承| MP[MyBatis-Plus ServiceImpl]
        Svc -->|实现| ISvc[IMasterDataService 30+方法]
        ISvc -->|继承| IBS[IBaseService]
        IBS -->|继承| MI[MyBatis-Plus IService]
    end

    subgraph DAO层
        Mpr[生成的Mapper] -->|继承| SM[SuperMapper]
        SM -->|继承| BM[MyBatis-Plus BaseMapper]
    end

    subgraph JPA层
        JPA[@Entity + ddl-auto=update] -.自动建表.-> DB[(Database)]
        JPA -.20MB依赖.-> App
    end

    MDSI -.自调用this.method().-> MDSI
    MDSI -.@Transactional失效.-> MDSI

    Ctrl -->|注入| Svc
    Svc -->|注入| Mpr

    style MDC fill:#ffcccc
    style MDSI fill:#ffcccc
    style IMDC fill:#ffcccc
    style JPA fill:#ffcccc
```

### 新架构（改造后）

```mermaid
graph TD
    subgraph Controller层
        Ctrl[生成的Controller 显式端点] -.可选继承.-> BC[BaseController 工具方法]
    end

    subgraph Business层 事务边界
        Biz[生成的BusinessImpl @Transactional] -->|继承| BB[BaseBusiness]
        Biz -->|实现| IBiz[IBusiness 接口]
    end

    subgraph Service层 薄层
        Svc[生成的ServiceImpl 无事务] -->|继承| MP[MyBatis-Plus ServiceImpl]
        Svc -->|实现| ISvc[XxxService extends IService]
    end

    subgraph DAO层 不变
        Mpr[生成的Mapper] -->|继承| SM[SuperMapper]
        SM -->|继承| BM[MyBatis-Plus BaseMapper]
    end

    subgraph Liquibase版本管理
        LQ[Liquibase changelog XML] -->|版本化DDL| DB[(Database)]
    end

    subgraph 多租户
        Tenant[TenantLineInnerInterceptor] -->|自动拼接tenant_id| Mpr
    end

    Ctrl -->|@Autowired| Biz
    Biz -->|@Autowired| Svc
    Biz -.内部方法调用.-> Biz
    Svc -->|注入| Mpr

    style Biz fill:#ccffcc
    style BB fill:#ccffcc
    style LQ fill:#cce5ff
```

---

## 开发工作流

### 首次初始化（JPA 一次性使用，在 micro-mysql 项目中）

```
1. 在 micro-mysql 项目中创建测试实体类，标注 JPA 注解
2. 启动应用（JPA ddl-auto=update 生效）
3. 验证数据库结构完整
4. 导出 Liquibase changelog：
   -> liquibase generateChangeLog
   -> 或 mvn liquibase:generateChangeLog
5. 生成的 V0__initial_schema.xml 放入 db/changelog/
```

### 移除 JPA

```
1. 确认 Liquibase V0 changelog 包含所有表/索引/约束
2. 删除 jbm-framework-autoconfigure-jpa 模块
3. 清理所有 pom.xml 中的 spring-boot-starter-data-jpa
4. 批量清理实体类中的 JPA 注解（@Entity, @MappedSuperclass, @Id, @Column 等）
5. 删除 @EntityScan 注解
```

### 日常开发（Liquibase 管理）

```
1. 新建实体类
   -> 仅标注 MyBatis-Plus 注解（@TableName, @TableId, @TableField）

2. 编写 Liquibase changelog
   -> 新增表：<changeSet> 包含 <createTable>
   -> 加字段：<changeSet> 包含 <addColumn>
   -> 放入 db/changelog/changes/

3. 运行代码生成器
   -> 生成 Mapper / Service / Business / Controller
   -> 同时生成 Liquibase changelog XML

4. 启动应用
   -> Liquibase 自动比对版本，执行未应用的 changelog
   -> 数据库结构同步
```

### 生产部署

```
应用启动 -> Liquibase 自动执行未应用的 changelog -> 版本记录清晰可追溯
```

---

## 验证策略

1. **单元测试**：为 `BaseBusiness` 的 CRUD 方法编写单元测试，验证事务回滚
2. **Liquibase 验证**：在 micro-mysql 项目中验证 changelog 生成和执行流程
3. **集成测试**：在 micro-mysql 项目中完成完整流程验证
4. **代码生成验证**：运行 `GenerateMasterData` 扫描测试 Entity，检查生成的 Controller 是否显式声明所有端点、BusinessImpl 是否包含 `@Transactional`、Changelog 是否正确生成
