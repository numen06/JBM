# JBM数据库版本控制

> **变更说明**：框架已移除 **Flyway**；结构版本化请统一使用 **Liquibase**，说明见仓库 `docs/masterdata-orm-stack-and-migration.md`。下文保留的历史描述中凡涉及 Flyway 的，请以 Liquibase 为准。

## 目录说明

本目录用于存放数据库版本升级脚本，JBM 框架会在应用启动时自动检测并执行这些脚本，实现数据库结构的版本化管理和自动升级。

## JBM 框架配置

JBM 框架提供了开箱即用的数据库版本控制功能，通过简单的配置即可启用。

### 基本配置

在 `application.yml` 或 `application.properties` 中配置：

```properties
# JBM SQL 自动执行配置（默认已启用，无需配置）
jbm.sql.auto-execute.enabled=true                    # 是否启用SQL自动执行（默认true）
jbm.sql.auto-execute.datasource-bean-name=           # 指定数据源Bean名称（可选，默认使用@Primary数据源）
jbm.sql.auto-execute.module-name=                    # 模块名称（可选，用于标识SQL文件来源）
```

**配置说明：**

1. **`jbm.sql.auto-execute.enabled`**
   - 类型：`Boolean`
   - 默认值：`true`
   - 说明：是否启用 SQL 自动执行功能。默认已启用，无需额外配置。

2. **`jbm.sql.auto-execute.datasource-bean-name`**
   - 类型：`String`
   - 默认值：`null`
   - 说明：指定要使用的数据源 Bean 名称。如果未指定，JBM 框架会自动选择合适的数据源：
     - 优先使用 MyBatis 配置的数据源
     - 其次使用 Spring 容器中 `@Primary` 标注的数据源

3. **`jbm.sql.auto-execute.module-name`**
   - 类型：`String`
   - 默认值：`null`
   - 说明：模块名称，用于标识 SQL 文件来源。通常无需配置，框架会自动处理。

### 默认配置

JBM 框架已内置了合理的默认配置，位于 `configs/mybatis-plus.properties`：

- 脚本位置：`classpath:db/migration`
- 脚本编码：`UTF-8`
- 首次运行已有数据库：自动创建基线（baseline）
- 脚本校验：启用校验，防止脚本被意外修改

**大多数情况下，使用默认配置即可，无需额外配置。**

### 配置示例

**单数据源场景（推荐）：**
```properties
# 使用默认配置，无需额外配置
# 或者显式启用（默认已启用）
jbm.sql.auto-execute.enabled=true
```

**多数据源场景：**
```properties
# 为主数据源启用（默认已启用）
jbm.sql.auto-execute.enabled=true

# 为其他数据源指定数据源名称（可选）
jbm.sql.auto-execute.datasource-bean-name=secondaryDataSource
```

## SQL 脚本命名规则

JBM 框架要求 SQL 脚本遵循以下命名规范：

### 标准命名格式

```
V{version}__{description}.sql
```

**命名规则说明：**

1. **版本号前缀**：必须以 `V` 开头（大写）
2. **版本号格式**：版本号可以使用以下格式：
   - 简单版本号：`V1`, `V2`, `V3` ...
   - 点分隔版本号：`V1.0`, `V1.1`, `V2.0` ...
   - 下划线分隔版本号：`V1_0`, `V1_1`, `V2_0` ...
   - 时间戳版本号：`V20240101120000`（推荐用于生产环境）
3. **分隔符**：版本号和描述之间使用**两个下划线** `__` 分隔
4. **描述信息**：描述信息使用下划线 `_` 分隔单词，不能包含空格
5. **文件扩展名**：必须是 `.sql`

### 命名示例

✅ **正确的命名：**
```
V1__Initial_schema.sql
V1.1__Add_user_table.sql
V1_2__Create_index.sql
V20240101120000__Add_new_column.sql
V2.0.0__Update_table_structure.sql
```

❌ **错误的命名：**
```
1__Initial_schema.sql          # 缺少 V 前缀
V1_Initial_schema.sql          # 只有一个下划线分隔符
V1 Initial schema.sql          # 描述中包含空格
V1__Initial schema.sql         # 描述中包含空格
v1__Initial_schema.sql         # V 必须大写
```

## SQL 脚本编写规范

### 1. SQL 脚本要求

- 脚本必须是可重复执行的（幂等性），建议使用 `IF NOT EXISTS`、`IF EXISTS` 等条件判断
- 每个脚本应该是一个完整的、可独立执行的数据库变更单元
- 框架会自动管理事务，无需在脚本中手动控制事务

### 2. 脚本示例

```sql
-- V1__Create_user_table.sql
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100),
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- V2__Add_user_index.sql
CREATE INDEX IF NOT EXISTS `idx_user_email` ON `user` (`email`);
```

### 3. 回滚脚本（可选）

如果需要支持回滚，可以创建对应的回滚脚本，命名格式为：

```
U{version}__{description}.sql
```

例如：`U2__Drop_user_index.sql`

> **注意**：回滚脚本功能需要额外配置，默认情况下 JBM 框架只执行版本升级脚本。

## 多数据源支持

JBM 框架支持多数据源场景，会自动为每个数据源执行相应的 SQL 脚本：

- **主数据源**：使用 `classpath:db/migration/` 目录下的脚本
- **其他数据源**：使用 `classpath:{数据源标识}/migration/` 目录下的脚本

**示例：**
- 主数据源：`classpath:db/migration/`
- 数据源 `db1`：`classpath:db1/migration/`
- 数据源 `db2`：`classpath:db2/migration/`

> **提示**：多数据源场景下，框架会自动识别并处理，无需额外配置。

## 执行流程

JBM 框架在应用启动时会自动执行以下流程：

1. **检测脚本**：自动扫描 `db/migration` 目录下的 SQL 脚本文件
2. **版本检查**：检查数据库中的版本历史表，确定哪些脚本已执行
3. **按序执行**：按版本号顺序执行所有未执行的脚本
4. **记录历史**：执行完成后，在版本历史表中记录执行记录

整个过程完全自动化，无需手动干预。

## 注意事项

### ⚠️ 重要警告

1. **不要修改已执行的脚本**：已执行的脚本不能修改，否则会导致校验失败，应用无法启动
2. **版本号必须递增**：新脚本的版本号必须大于已执行脚本的版本号
3. **脚本必须可重复执行**：建议使用 `IF NOT EXISTS`、`IF EXISTS` 等条件判断，确保脚本的幂等性
4. **生产环境谨慎操作**：在生产环境执行前，务必在测试环境充分测试

### 首次使用已有数据库

如果数据库已经存在数据，首次使用 JBM 数据库版本控制功能时：

1. 框架会自动创建基线（baseline），将当前数据库状态作为初始版本
2. 之后只会执行版本号大于基线的脚本
3. 无需手动操作，框架会自动处理

### 版本历史表

JBM 使用 **Liquibase**。框架会在数据库中自动创建版本历史表（如 `DATABASECHANGELOG`、`DATABASECHANGELOGLOCK`）来记录脚本执行历史，包含以下信息：
- 执行顺序、版本号、描述信息
- 脚本文件名、校验和
- 执行时间、执行耗时、执行状态

该表由框架自动管理，无需手动维护。

## 常见问题

### Q1: 如何跳过某个已执行的脚本？

A: 不要删除或修改已执行的脚本文件。如果需要修复问题，创建一个新的版本号更大的脚本来修复或回滚之前的变更。

### Q2: 脚本执行失败怎么办？

A: 框架会在版本历史表中记录失败的脚本。修复脚本后，需要：
1. 修复数据库状态（手动执行修复 SQL 或回滚）
2. 清理版本历史表中的失败记录
3. 重新启动应用，框架会重新执行修复后的脚本

### Q3: 如何查看脚本执行历史？

A: 查询数据库中的 Liquibase 表 `DATABASECHANGELOG`，或查看应用启动日志中的 Liquibase 输出。

### Q4: 可以在脚本中使用存储过程吗？

A: 可以，但需要注意数据库方言的兼容性。建议将存储过程定义和调用分开到不同的脚本中，便于维护和调试。

### Q5: 如何禁用数据库版本控制功能？

A: 在配置文件中设置 `jbm.sql.auto-execute.enabled=false` 即可禁用。

## 相关文档

- 仓库根目录：`docs/masterdata-orm-stack-and-migration.md`（Liquibase + MyBatis-Plus 与迁移指引）
