---
name: Masterdata模块架构升级
overview: 重构 jbm-framework-data-masterdata 模块，消除Controller深度继承链，重新设计DAO/Service/Business三层架构，修复事务自调用失效问题，移除JPA/Hibernate只保留MyBatis-Plus，升级代码生成器从"空壳+继承"模式改为"完整代码生成"模式，建立Flyway数据库版本管理替代JPA自动建表。
todos:
  - id: phase0-remove-jpa-dependencies
    content: 移除 JPA/Hibernate 依赖：删除 jbm-framework-autoconfigure-jpa 和 jbm-framework-autoconfigure-jpa-h2 模块的 spring-boot-starter-data-jpa 依赖
    status: pending
  - id: phase0-clean-entity-annotations
    content: 清除实体基类中的 JPA 注解（@MappedSuperclass、@Id、@GeneratedValue），保留 MyBatis-Plus 注解
    status: pending
  - id: phase0-generate-ddl-scripts
    content: 生成所有实体的 DDL 迁移脚本（替代 JPA ddl-auto=update），使用 Flyway V1__init.sql 格式
    status: pending
  - id: phase0-enable-flyway
    content: 启用 Flyway：将 spring.flyway.enabled 改为 true，配置迁移脚本路径
    status: pending
  - id: phase1-delete-old-controller
    content: 删除旧的 Controller Collection 基类和接口（BaseCollection、MasterDataCollection、MasterDataTreeCollection、MultiPlatformCollection、IMasterDataController 等）
    status: pending
  - id: phase1-new-base-controller
    content: 新建轻量 BaseController 工具基类（仅提供响应封装、校验辅助，不提供端点实现）
    status: pending
  - id: phase2-refactor-service-interfaces
    content: 精简 Service 层：删除 IMasterDataService、IMultiPlatformService，精简 IBaseService、IMasterDataTreeService
    status: pending
  - id: phase2-refactor-service-impls
    content: 重构 ServiceImpl 基类：删除 MasterDataServiceImpl/MasterDataTreeServiceImpl/MultiPlatformServiceImpl，精简 BaseServiceImpl
    status: pending
  - id: phase3-create-business-layer
    content: 新建 BaseBusiness 和 TreeBusiness 基类，所有写方法标注 @Transactional(rollbackFor = Exception.class)
    status: pending
  - id: phase3-delete-old-business-transaction
    content: 删除旧的 Business 空壳（IPlatformBusiness、PlatformBusinessImpl）和旧事务 AOP 配置（TransactionAdviceConfig、EnableTransactionAdviceManagement）
    status: pending
  - id: phase4-update-templates
    content: 升级所有 .btl 模板：controller 模板显式声明端点，service 模板继承 IService，business 模板包含完整 CRUD + @Transactional
    status: pending
  - id: phase5-update-generators
    content: 升级代码生成器类：GenerateControllerCode/GenerateServiceCode/GenerateServiceImplCode/GenerateBusinessCode/GenerateBusinessImplCode，删除继承映射逻辑
    status: pending
  - id: phase5-update-generate-masterdata
    content: 更新 GenerateMasterData.java：调整生成顺序和模块控制，简化 SuperClass 解析逻辑
    status: pending
  - id: phase6-migration-test
    content: 选取 jbm-cluster 中一个模块进行迁移验证，确保新架构可编译和运行
    status: pending
isProject: false
---

# Masterdata 模块架构升级计划

## 问题分析

### 当前架构痛点

1. **Controller 深度继承链**：`Controller -> MasterDataCollection -> BaseCollection`（3层），所有CRUD端点隐式继承，难以理解和调试
2. **Service 方法膨胀**：`IMasterDataService` 定义 30+ 方法，`MasterDataServiceImpl` 实现大量与 MyBatis-Plus `IService` 重叠的方法
3. **事务自调用失效**：`MasterDataServiceImpl` 中 `this.deleteById(id)`、`this.saveOrUpdateBatch()` 等自调用绕过 Spring AOP 代理，`@Transactional` 不生效
4. **Business 层形同虚设**：`IPlatformBusiness` 和 `PlatformBusinessImpl` 是空壳，没有实际作用
5. **代码生成器产空壳**：生成的 Controller/Service/Mapper 全是空类，所有逻辑在框架基类中
6. **JPA 与 MyBatis-Plus 双 ORM 并存**：所有实体类同时标注 JPA（@Entity、@Id、@Column）和 MyBatis-Plus（@TableName、@TableId、@TableField）注解，引入 Hibernate 仅用于 ddl-auto=update 自动建表，增加约 20MB 依赖体积和启动时间
7. **JPA 自动建表不可控**：`ddl-auto=update` 只能添加新列，不能删除/修改已有列，索引和约束管理不可控

---

## 移除 JPA / 自动建表建库方案

### 现状

当前项目中 JPA/Hibernate 的作用：
- **唯一实际用途**：`spring.jpa.hibernate.ddl-auto=update` 自动根据 @Entity 注解创建和更新表
- **不用于数据操作**：所有 CRUD 都通过 MyBatis-Plus，没有使用 JPA Repository
- **带来代价**：引入约 20MB 的 Hibernate 依赖、延长启动时间、双重注解维护负担

### 移除 JPA 后自动建表建库的替代方案

**采用 Flyway 数据库版本管理**（框架已集成，默认关闭）

#### 方案设计

```
开发阶段：实体字段变更 -> 手动编写 DDL 脚本 -> Flyway 版本化执行
生产阶段：Flyway 自动执行未应用的迁移脚本 -> 表结构升级
```

#### 具体实施

##### 1. 实体基类清除 JPA 注解

所有 `@MappedSuperclass` 基类中的 JPA 注解需要清除：

| 文件 | 清除的注解 | 保留的注解 |
|------|-----------|-----------|
| `MasterDataEntity.java` | `@MappedSuperclass` | `@TableName` |
| `MasterDataIdEntity.java` | `@MappedSuperclass`, `@Id`, `@GeneratedValue` | `@TableId(type = IdType.ASSIGN_ID)` |
| `MasterDataTreeEntity.java` | `@MappedSuperclass` | `@TableName` |
| `MultiPlatformEntity.java` | `@MappedSuperclass` | `@TableName` |
| `MultiPlatformIdEntity.java` | `@MappedSuperclass`, `@Id`, `@GeneratedValue` | `@TableId` |

业务实体类中的 JPA 注解清除：

| 注解 | 替代方案 |
|------|----------|
| `@Entity` | **删除**，MyBatis-Plus 不需要 |
| `@Table(name=..., indexes=...)` | 表名用 `@TableName`，索引/约束在 DDL 脚本中定义 |
| `@Id` | 保留 `@TableId`，删除 `@Id` |
| `@GeneratedValue` | **删除**，主键策略由 `@TableId(type=...)` 控制 |
| `@Column(name=..., columnDefinition=...)` | 字段名用 `@TableField`，类型/长度在 DDL 脚本中定义 |
| `@Column(unique=...)` | 唯一约束在 DDL 脚本中定义 |
| `@Enumerated` | MyBatis-Plus 通过 TypeHandler 处理枚举，在 DDL 中定义列类型 |
| `@Lob` | 在 DDL 脚本中定义 TEXT/LONGTEXT 类型 |
| `@Transient` | 改用 `@TableField(exist = false)` |

##### 2. DDL 脚本生成策略

**方式一：从现有数据库反向导出（推荐用于首次迁移）**

在现有运行环境中执行：
```bash
mysqldump -u root -p --no-data --skip-triggers db_name > V1__init_schema.sql
```
或使用 MyBatis-Plus 的 `com.baomidou.mybatisplus.core.toolkit.StringUtils` 配合实体元数据生成。

**方式二：按实体类别手动编写（新实体）**

代码生成器扩展：新增 `GenerateDdlCode.java` 生成器，根据实体类字段自动生成 DDL 脚本模板。

##### 3. DDL 脚本规范

每个模块的 `src/main/resources/db/migration/` 目录：

```
db/migration/
  V1__init_base_tables.sql          # 基础表（BaseUser, BaseApp 等）
  V2__add_base_dic_table.sql        # 新增表
  V3__alter_base_user_add_column.sql # 修改表
  V4__create_index_on_base_app.sql  # 新增索引
```

命名规范：
- `V{version}__{description}.sql`（version 为数字递增，description 用下划线分隔）
- 首次建表：`V1__init_schema.sql`（所有 48 张表的完整 DDL）
- 后续变更：`V2__add_xxx_table.sql` 或 `V3__alter_xxx_add_column.sql`

##### 4. DDL 脚本内容模板

**建表语句模板**：
```sql
-- V1__init_base_area.sql
CREATE TABLE IF NOT EXISTS `base_area` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `area_code` VARCHAR(64) NOT NULL COMMENT '区域编码',
    `area_name` VARCHAR(128) NOT NULL COMMENT '区域名称',
    `parent_code` VARCHAR(64) DEFAULT NULL COMMENT '父级编码',
    `area_type` VARCHAR(32) DEFAULT NULL COMMENT '区域类型',
    `full_pin_yin` VARCHAR(256) DEFAULT NULL COMMENT '全拼音',
    `simple_pin_yin` VARCHAR(64) DEFAULT NULL COMMENT '简拼',
    `center_location` VARCHAR(64) DEFAULT NULL COMMENT '中心位置坐标',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_area_code` (`area_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';
```

**修改表语句模板**：
```sql
-- V2__alter_base_user_add_email.sql
ALTER TABLE `base_user` ADD COLUMN `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱' AFTER `phone`;
ALTER TABLE `base_user` ADD INDEX `idx_email` (`email`);
```

##### 5. 字段类型映射规则（Java -> MySQL）

| Java 类型 | MySQL 类型 | 备注 |
|-----------|-----------|------|
| `Long` / `long` | `BIGINT` | 主键/外键 |
| `Integer` / `int` | `INT` | |
| `String` | `VARCHAR(255)` | 默认长度 |
| `String` + `@Column(columnDefinition="TEXT")` | `TEXT` | 大文本 |
| `String` + `@Lob` | `LONGTEXT` | 超大文本（规则内容、BPMN等） |
| `Date` / `LocalDateTime` | `DATETIME` | 时间字段 |
| `Boolean` / `boolean` | `TINYINT(1)` | 布尔值 |
| `Double` / `double` | `DOUBLE` | |
| `Enum` | `VARCHAR(64)` | 枚举存储为字符串 |
| `UUID` / `String` (ID) | `VARCHAR(64)` | UUID 主键 |

##### 6. 实体基类继承字段的 DDL 包含规则

每个具体实体需要在 DDL 中包含其继承链的所有字段：

| 继承基类 | 继承字段 |
|----------|----------|
| `MasterDataEntity` | `create_time DATETIME`, `update_time DATETIME` |
| `MasterDataIdEntity` | 上述 + `id BIGINT PRIMARY KEY` |
| `MasterDataTreeEntity` | 上述 + `parent_id BIGINT`, `level INT`, `leaf_path VARCHAR(512)` |
| `MasterDataCodeEntity` | `id BIGINT` + `code VARCHAR(64)` |
| `MultiPlatformEntity` | `create_time`, `update_time` + `app_id BIGINT` |
| `MultiPlatformIdEntity` | 上述 + `id BIGINT` |

##### 7. Flyway 配置变更

```properties
# 原配置（mybatis-plus.properties）
spring.flyway.enabled=false
# 改为：
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.clean-disabled=true
spring.flyway.baseline-version=1
```

##### 8. 需要删除的 JPA 相关模块

| 模块/文件 | 操作 |
|-----------|------|
| `jbm-framework-autoconfigure-jpa/` | **删除整个模块** |
| `jbm-framework-autoconfigure-jpa-h2/` | **删除整个模块** |
| `jbm-framework-dependencies/pom.xml` | 删除 `spring-boot-starter-data-jpa` 依赖管理 |
| `jbm-cluster-api-basic/pom.xml` | 删除 `spring-boot-starter-data-jpa` 依赖 |
| `jpa.properties` | **删除** |
| 各 Application 类中的 `@EntityScan` | **删除**（不再需要 JPA 实体扫描） |

##### 9. 新实体的建表流程（开发体验）

```
1. 编写实体类（仅 MyBatis-Plus 注解）
   -> @TableName("xxx"), @TableId, @TableField

2. 运行代码生成器
   -> 生成 Mapper / Service / Business / Controller
   -> 同时生成 DDL 脚本模板到 db/migration/V{version}__create_xxx.sql

3. 启动应用
   -> Flyway 自动执行 DDL 脚本建表

4. 字段变更时
   -> 新增 V{next}__alter_xxx.sql 脚本
   -> Flyway 自动执行变更
```

##### 10. 代码生成器扩展：DDL 脚本生成

新增 `GenerateDdlCode.java` 生成器：
- 扫描实体类字段
- 根据字段类型、注解（如 `@Column(columnDefinition="TEXT")` 改为自定义 `@DdlColumn(type="TEXT")` 注解或直接解析字段类型）
- 生成对应 DDL SQL 脚本
- 自动递增版本号

---

## 新架构设计

### 分层原则

```
Controller 层 -> 显式声明端点，委托给 Business 层
     |
Business 层 -> 事务边界 + 业务编排 + CRUD 实现（通过 Service 或直接通过 Mapper）
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
- 移除 `TransactionAdviceConfig` 的 AOP 自动事务（因为 `txAdviceAdvisor` 对同类自调用同样无效），改为显式 `@Transactional`

---

## 实施步骤

### Phase 1: 重新设计实体层（保持兼容）

**文件**: `jbm-framework-data-masterdata/src/main/java/com/jbm/framework/masterdata/usage/entity/`

实体基类（`MasterDataEntity`、`MasterDataIdEntity`、`MasterDataTreeEntity` 等）**保持不变**，确保现有业务代码无需修改实体定义。

### Phase 2: 重新设计 DAO 层（保持不变）

**文件**: `jbm-framework-data-masterdata/src/main/java/com/jbm/framework/masterdata/mapper/SuperMapper.java`

SuperMapper **保持不变**，继续继承 `BaseMapper<T>`，提供 `pageList`、`getEntityMap`、`selectPageList` 三个扩展方法。

### Phase 3: 重新设计 Service 层（精简）

**目标**: 消除与 MyBatis-Plus `IService` 的大量重叠，Service 变为薄层。

#### 3.1 删除/精简 Service 接口

| 文件 | 操作 |
|------|------|
| `IBaseService.java` | **精简**：只保留 `pageList`、`getEntityMap` 等 MyBatis-Plus 未提供的方法 |
| `IMasterDataService.java` | **删除**：方法要么已存在于 `IService`，要么移入 Business 层 |
| `IMasterDataTreeService.java` | **精简**：只保留树形特有方法签名（`selectRootListById`、`selectChildNodesById` 等） |
| `IMultiPlatformService.java` | **删除**：空接口无意义 |
| `IBaseSqlService.java` | **评估后删除或保留** |

新的 Service 接口范式：
```java
public interface XxxService extends IService<Xxx> {
    // 仅声明自定义查询方法，CRUD 全部来自 IService
    Page<Xxx> pageCustom(CriteriaQueryWrapper<Xxx> wrapper);
    // ... 其他自定义方法
}
```

#### 3.2 重构 ServiceImpl 基类

| 文件 | 操作 |
|------|------|
| `BaseServiceImpl.java` | **精简**：移除与 MyBatis-Plus `ServiceImpl` 重叠的方法，保留 `pageList`（XML 外键连接）等独有功能 |
| `MasterDataServiceImpl.java` | **删除**：所有 `@Transactional` 标注的方法移到 Business 层；其余重复方法删除 |
| `MasterDataTreeServiceImpl.java` | **精简**：保留树形查询方法，**移除所有 `@Transactional`** |
| `MultiPlatformServiceImpl.java` | **删除**：空壳类 |

新的 ServiceImpl 范式：
```java
@Service
public class XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx> implements XxxService {
    // 仅实现自定义查询方法
    // 无 @Transactional 注解
}
```

### Phase 4: 新增 Business 层（事务边界 + 业务编排）

#### 4.1 创建 Business 基类

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

新建 `TreeBusiness<Entity, Service extends IMasterDataTreeService>` 扩展树形操作：
```java
public abstract class TreeBusiness<...> extends BaseBusiness<...> {
    public List<Entity> selectRootList() { ... }
    public List<Entity> selectTree() { ... }
}
```

#### 4.2 删除旧的 Business 空壳

| 文件 | 操作 |
|------|------|
| `IPlatformBusiness.java` | **删除** |
| `PlatformBusinessImpl.java` | **删除** |

#### 4.3 删除旧的事务 AOP 配置

| 文件 | 操作 |
|------|------|
| `TransactionAdviceConfig.java` | **删除**（AOP 事务对同类自调用无效） |
| `EnableTransactionAdviceManagement.java` | **删除** |

### Phase 5: 重新设计 Controller 层（去除继承）

#### 5.1 删除旧的 Collection 基类

| 文件 | 操作 |
|------|------|
| `BaseCollection.java` | **删除** |
| `MasterDataCollection.java` | **删除** |
| `MasterDataTreeCollection.java` | **删除** |
| `MultiPlatformCollection.java` | **删除** |

删除 Controller 接口：
| 文件 | 操作 |
|------|------|
| `IMasterDataController.java` | **删除** |
| `IMasterDataTreeController.java` | **删除** |
| `IMultiPlatformController.java` | **删除** |
| `IMultiPlatformTreeController.java` | **删除** |

#### 5.2 创建新的 BaseController（轻量工具基类）

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

#### 5.3 Controller 模板改为显式声明

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

**关键变化**：
- 所有端点显式声明，一目了然
- 业务方法直接添加在 Controller 中（不再需要绕开 Collection 类）
- 委托给 Business 层处理逻辑

### Phase 6: 升级代码生成器

#### 6.1 修改模板文件

| 模板文件 | 修改内容 |
|----------|----------|
| `controller.btl` | 改为显式声明所有 CRUD 端点 + `@Autowired` 注入 Business，不再 `extends Collection` |
| `service.btl` | 改为 `extends IService<Entity>`，仅保留自定义方法声明位置 |
| `serviceImpl.btl` | 改为 `extends ServiceImpl<Mapper, Entity>`，无 `@Transactional` |
| `mapper.btl` | **保持不变** |
| `business.btl` | 改为接口声明，继承 `BaseBusiness` |
| `businessImpl.btl` | 改为实现类，包含 CRUD 方法实现 + `@Transactional` |
| `mapperXml.btl` | **保持不变** |

新增模板：
| 模板文件 | 说明 |
|----------|------|
| `baseBusiness.btl` | 生成 `XxxBusiness` 接口（继承 `BaseBusiness<Entity, Service>`） |
| `baseBusinessImpl.btl` | 生成 `XxxBusinessImpl`（实现 CRUD + 事务，标注 `@Transactional`） |

#### 6.2 修改生成器逻辑

| 生成器类 | 修改内容 |
|----------|----------|
| `GenerateControllerCode.java` | 删除 `getSuperClass()` 继承映射，改为生成完整 Controller 代码 |
| `GenerateServiceCode.java` | 修改 `extClass` 为 `IService`，不再根据实体类型选择不同父接口 |
| `GenerateServiceImplCode.java` | 修改 `extClass` 为 `ServiceImpl`，简化逻辑 |
| `GenerateBusinessCode.java` | 新增/修改：生成 Business 接口 |
| `GenerateBusinessImplCode.java` | 新增/修改：生成包含完整 CRUD 实现的 BusinessImpl |

修改 `GenerateMasterData.java`：
- `enableBusiness` 默认值改为 `true`（原来就是 true，确保始终生成）
- 调整生成顺序：Mapper -> Service -> ServiceImpl -> Business -> BusinessImpl -> Controller

#### 6.3 删除不再需要的代码

| 文件/类 | 操作 |
|---------|------|
| `CodeType.business` / `CodeType.businessImpl` 的旧映射逻辑 | 清理 |
| `BussinessGroup` 相关逻辑 | 评估：如果不再需要按业务分组，可删除；如果保留，调整模板 |
| `SuperClass` 解析逻辑 | 简化：不再需要根据实体父类决定 Service/Controller 继承哪个基类 |

---

## 迁移影响和兼容性

### 对现有业务代码的影响

| 层级 | 影响 | 迁移策略 |
|------|------|----------|
| Entity | 无影响 | 保持不变 |
| Mapper | 无影响 | 保持不变 |
| Service 接口 | 低影响 | `IMasterDataService` 删除后，现有接口需改为 `extends IService` |
| ServiceImpl | 中影响 | `extends MasterDataServiceImpl` 改为 `extends ServiceImpl`，自定义方法保留 |
| Controller | **高影响** | `extends MasterDataCollection` 需改为显式声明端点（可自动生成） |
| Business | 无影响 | 旧 Business 为空壳，无实际使用 |

### 迁移辅助

1. 在框架升级完成后，提供一个 **迁移脚本/工具**，扫描 jbm-cluster 中的旧 Controller，自动生成新格式的显式端点代码
2. ServiceImpl 的迁移可以手动进行：将 `extends MasterDataServiceImpl<Xxx>` 改为 `extends ServiceImpl<XxxMapper, Xxx>`，删除被基类覆盖的方法（因为新基类不再提供这些方法，如果业务代码有调用则需要加回或改用 Business 层）

---

## 文件变更清单

### 删除的文件

| 文件路径 | 说明 |
|----------|------|
| `.../web/BaseCollection.java` | 空壳基类 |
| `.../web/MasterDataCollection.java` | Controller 继承基类 |
| `.../web/MasterDataTreeCollection.java` | 树形 Controller 基类 |
| `.../web/MultiPlatformCollection.java` | 多平台 Controller 基类 |
| `.../controller/IMasterDataController.java` | Controller 接口 |
| `.../controller/IMasterDataTreeController.java` | 树形 Controller 接口 |
| `.../controller/IMultiPlatformController.java` | 多平台 Controller 接口 |
| `.../service/IMasterDataService.java` | 膨胀的 Service 接口 |
| `.../service/IMultiPlatformService.java` | 空 Service 接口 |
| `.../mybatis/MasterDataServiceImpl.java` | 带事务的 ServiceImpl |
| `.../mybatis/MasterDataTreeServiceImpl.java` | 树形 ServiceImpl |
| `.../mybatis/MultiPlatformServiceImpl.java` | 空 ServiceImpl |
| `.../business/IPlatformBusiness.java` | 空 Business 接口 |
| `.../business/PlatformBusinessImpl.java` | 空 Business 实现 |
| `.../transaction/TransactionAdviceConfig.java` | AOP 事务配置（对自调用无效） |
| `.../transaction/EnableTransactionAdviceManagement.java` | 事务管理注解 |

### 新建的文件

| 文件路径 | 说明 |
|----------|------|
| `.../business/BaseBusiness.java` | 新 Business 基类（事务边界） |
| `.../business/TreeBusiness.java` | 树形 Business 基类 |
| `.../web/BaseController.java` | 轻量 Controller 工具基类 |

### 修改的文件

| 文件路径 | 说明 |
|----------|------|
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
| `.../code/GenerateMasterData.java` | 调整生成顺序和模块控制 |

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

    MDSI -.自调用this.method().-> MDSI
    MDSI -.@Transactional失效.-> MDSI

    Ctrl -->|注入| Svc
    Svc -->|注入| Mpr

    style MDC fill:#ffcccc
    style MDSI fill:#ffcccc
    style IMDC fill:#ffcccc
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

    Ctrl -->|@Autowired| Biz
    Biz -->|@Autowired| Svc
    Biz -.内部方法调用.-> Biz
    Svc -->|注入| Mpr

    style Biz fill:#ccffcc
    style BB fill:#ccffcc
```

关键变化：
- 事务统一在 **Business 层**，`@Transactional` 在 Business 方法上
- Service 层无事务，只做查询/转换
- Controller 显式声明端点，委托给 Business
- 同类内方法调用在 Business 层内通过 `this.method()` 时，因为整个方法已被 `@Transactional` 覆盖，事务自然生效

---

## 验证策略

1. **单元测试**：为 `BaseBusiness` 的 CRUD 方法编写单元测试，验证事务回滚
2. **集成测试**：选取 jbm-cluster 中的一个模块（如 `jbm-cluster-platform-center`），手动迁移一个 Entity 的完整四层代码，验证编译和运行
3. **代码生成验证**：运行 `GenerateMasterData` 扫描测试 Entity，检查生成的 Controller 是否显式声明所有端点、BusinessImpl 是否包含 `@Transactional`
