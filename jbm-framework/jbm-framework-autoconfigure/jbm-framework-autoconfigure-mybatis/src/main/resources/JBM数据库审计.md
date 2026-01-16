# JBM数据库审计

## 功能概述

JBM 框架提供了强大的 SQL 审计功能，可以自动拦截、记录和分析所有数据库操作，支持多种推送方式，帮助开发者监控 SQL 执行情况、定位性能问题、追踪数据变更。

## 核心特性

- ✅ **自动拦截**：自动拦截所有 MyBatis SQL 执行，无需修改业务代码
- ✅ **多种记录模式**：支持白名单模式和普通模式
- ✅ **慢查询检测**：自动识别并标记慢查询
- ✅ **多种推送方式**：支持本地日志、数据库存储、消息队列、HTTP 推送
- ✅ **灵活过滤**：支持自定义过滤规则，过滤无用 SQL
- ✅ **详细执行信息**：记录 SQL 语句、参数、执行时间、结果等完整信息
- ✅ **异常追踪**：自动记录 SQL 执行异常信息

## JBM 框架配置

JBM 框架提供了开箱即用的数据库审计功能，通过简单的配置即可启用。

### 基本配置

在 `application.yml` 或 `application.properties` 中配置：

```properties
# SQL 日志基础配置
sql-log.mode=WHITELIST                    # 记录模式：WHITELIST（白名单模式）或 NORMAL（普通模式）
sql-log.whitelist[0]=com.example.mapper.* # 白名单模式下的 Mapper 方法匹配规则（支持 Ant 路径匹配）
sql-log.format=MERGED                     # 日志格式：MERGED（合并格式）或 OFFICIAL（官方格式）
sql-log.filter=true                        # 是否启用 SQL 过滤（默认 true）
sql-log.exclude=SELECT 1;|SHOW VARIABLES;|SET .*  # SQL 排除规则（正则表达式）

# SQL 审计配置
sql-log.audit.enabled=true                # 是否启用审计（默认 true）
sql-log.audit.push-type=LOCAL_LOG         # 审计推送方式：LOCAL_LOG、DATABASE、MESSAGE_QUEUE、HTTP
sql-log.audit.enable-local-log=true       # 是否启用本地打印（默认 true）

# 慢查询配置
sql-log.slow-query.enabled=true           # 是否启用慢查询检测（默认 true）
sql-log.slow-query.threshold=3000         # 慢查询阈值（毫秒，默认 3000）
sql-log.slow-query.log-slow-query=true    # 是否打印慢查询日志（默认 true）
```

### 配置说明

#### 1. 记录模式（mode）

- **WHITELIST（白名单模式）**：只记录白名单中匹配的 SQL
  - 需要配置 `whitelist` 列表
  - 适合生产环境，减少日志量
  - 默认模式，保持向后兼容

- **NORMAL（普通模式）**：记录所有 SQL（除了被过滤的）
  - 不需要配置 `whitelist`
  - 适合开发环境，便于调试
  - 会自动应用过滤规则

#### 2. 白名单配置（whitelist）

仅在 `WHITELIST` 模式下有效，支持 Ant 路径匹配模式：

```properties
# 匹配所有 Mapper
sql-log.whitelist[0]=com.example.mapper.*

# 匹配特定包下的所有 Mapper
sql-log.whitelist[0]=com.example.mapper.user.*
sql-log.whitelist[1]=com.example.mapper.order.*

# 匹配特定方法
sql-log.whitelist[0]=com.example.mapper.UserMapper.selectById
```

#### 3. 日志格式（format）

- **MERGED（合并格式）**：SQL 和参数合并显示，一行输出
  - 格式：`2024-01-01 12:00:00 | DS: dataSource | [SUCCESS] | took 10ms | SELECT * FROM user WHERE id = 1`
  - 默认格式，简洁明了

- **OFFICIAL（官方格式）**：分别显示 Preparing 和 Parameters
  - 格式类似 MyBatis 官方日志格式
  - 适合需要详细参数信息的场景

#### 4. 自定义日志格式（customFormat）

支持自定义日志格式化字符串，类似 p6spy：

```properties
sql-log.custom-format=%(currentTime) | DS: %(dataSource) | [%(result)] | took %(executionTime)ms | %(sql)%(errorMessage)
```

**支持的占位符：**
- `%(currentTime)` - 当前时间
- `%(dataSource)` - 数据源名称
- `%(executionTime)` - 执行时间（毫秒）
- `%(sql)` - SQL 语句
- `%(mapperId)` - Mapper 方法全限定名
- `%(operationType)` - 操作类型（query/update）
- `%(applicationName)` - 应用名称
- `%(instanceId)` - 实例ID
- `%(slowQuery)` - 是否慢查询
- `%(result)` - 执行结果（SUCCESS/FAILED）
- `%(errorMessage)` - 错误信息

#### 5. SQL 过滤（filter）

启用后会自动过滤掉无用的 SQL 语句（如 `SELECT 1`、`SHOW VARIABLES` 等），减少日志噪音。

**排除规则（exclude）**：支持正则表达式，多个规则用 `|` 分隔：

```properties
sql-log.exclude=SELECT 1;|SHOW VARIABLES;|SET .*
```

#### 6. 慢查询检测（slow-query）

自动检测执行时间超过阈值的 SQL，并标记为慢查询。慢查询不受白名单限制，始终会被记录。

```properties
sql-log.slow-query.enabled=true      # 启用慢查询检测
sql-log.slow-query.threshold=3000     # 阈值：3000 毫秒（3秒）
sql-log.slow-query.log-slow-query=true # 打印慢查询日志
```

#### 7. 审计推送方式（audit.push-type）

支持多种推送方式，可以同时启用多种方式：

- **LOCAL_LOG（本地打印）**：默认方式，输出到应用日志
- **DATABASE（数据库存储）**：存储到数据库表中
- **MESSAGE_QUEUE（消息队列）**：推送到 Kafka 或 RabbitMQ
- **HTTP（HTTP 推送）**：通过 HTTP 接口推送

### 配置示例

#### 示例 1：基础配置（白名单模式）

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: LOCAL_LOG
  slow-query:
    enabled: true
    threshold: 3000
```

#### 示例 2：普通模式（记录所有 SQL）

```yaml
sql-log:
  mode: NORMAL
  filter: true
  exclude: "SELECT 1;|SHOW VARIABLES;|SET .*"
  audit:
    enabled: true
    push-type: LOCAL_LOG
  slow-query:
    enabled: true
    threshold: 2000
```

#### 示例 3：数据库存储审计

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: DATABASE
    enable-local-log: true  # 同时保留本地打印
    database:
      enabled: true
      table-name: sql_audit_log  # 可选，默认表名
```

#### 示例 4：消息队列推送

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: MESSAGE_QUEUE
    message-queue:
      enabled: true
      topic: sql-audit-topic  # Kafka Topic 或 RabbitMQ Exchange
      exchange: sql-audit-exchange  # RabbitMQ Exchange（可选）
      routing-key: sql.audit  # RabbitMQ Routing Key（可选）
```

#### 示例 5：HTTP 推送

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: HTTP
    http:
      enabled: true
      url: http://log-server/api/sql-audit
      timeout: 5000  # 请求超时时间（毫秒）
      async: true    # 是否异步推送（默认 true）
```

#### 示例 6：多种推送方式组合

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: MULTIPLE
    enable-local-log: true  # 本地打印
    database:
      enabled: true         # 数据库存储
    http:
      enabled: true         # HTTP 推送
      url: http://log-server/api/sql-audit
      async: true
```

## 功能详解

### 1. SQL 拦截机制

JBM 框架通过 MyBatis 拦截器（`SqlSessionInterceptor`）自动拦截所有 SQL 执行：

- **拦截时机**：SQL 执行前和执行后
- **拦截范围**：所有通过 MyBatis 执行的 SQL（包括查询和更新）
- **性能影响**：最小化性能开销，异步处理审计信息

### 2. 执行信息收集

审计功能会收集以下信息：

- **SQL 信息**：
  - 原始 SQL（包含占位符）
  - 可读 SQL（参数已替换）
  - SQL 参数列表
  - 格式化参数字符串

- **执行信息**：
  - 执行开始时间
  - 执行结束时间
  - 执行耗时（毫秒）
  - 操作类型（query/update）
  - 执行结果（成功/失败）

- **应用信息**：
  - 应用名称
  - 实例ID
  - 主机名
  - IP 地址
  - 端口号

- **查询结果**（仅查询操作）：
  - 查询结果对象
  - 结果行数

- **异常信息**（如果执行失败）：
  - 错误消息
  - 异常堆栈（简化后）

### 3. 慢查询检测

慢查询检测功能会：

1. **自动检测**：在执行完成后自动检测执行时间
2. **阈值判断**：与配置的阈值进行比较
3. **标记慢查询**：超过阈值的 SQL 会被标记为慢查询
4. **绕过白名单**：慢查询不受白名单限制，始终会被记录
5. **特殊标识**：在日志中会明确标识为慢查询

**慢查询日志示例：**
```
2024-01-01 12:00:00 | DS: dataSource | [SLOW_QUERY] | took 5000ms | SELECT * FROM large_table WHERE ...
```

### 4. 推送方式详解

#### 4.1 本地打印（LOCAL_LOG）

默认推送方式，将审计信息输出到应用日志中。

**特点：**
- 无需额外配置
- 实时输出
- 与现有日志系统集成
- 支持日志级别控制

**配置：**
```yaml
sql-log:
  audit:
    enabled: true
    push-type: LOCAL_LOG
    enable-local-log: true  # 默认 true
```

#### 4.2 数据库存储（DATABASE）

将审计信息存储到数据库表中，便于后续查询和分析。

**特点：**
- 持久化存储
- 支持复杂查询
- 便于统计分析
- 需要创建对应的表结构

**配置：**
```yaml
sql-log:
  audit:
    enabled: true
    push-type: DATABASE
    database:
      enabled: true
      table-name: sql_audit_log  # 可选，默认表名
```

**表结构建议：**
```sql
CREATE TABLE sql_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mapper_id VARCHAR(255),
    original_sql TEXT,
    readable_sql TEXT,
    operation_type VARCHAR(20),
    execution_time BIGINT,
    success BOOLEAN,
    error_message TEXT,
    application_name VARCHAR(100),
    instance_id VARCHAR(100),
    hostname VARCHAR(100),
    ip VARCHAR(50),
    port VARCHAR(10),
    slow_query BOOLEAN,
    slow_query_threshold BIGINT,
    start_time BIGINT,
    end_time BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mapper_id (mapper_id),
    INDEX idx_execution_time (execution_time),
    INDEX idx_slow_query (slow_query),
    INDEX idx_created_time (created_time)
);
```

#### 4.3 消息队列推送（MESSAGE_QUEUE）

将审计信息推送到消息队列（Kafka 或 RabbitMQ），实现异步处理和分布式收集。

**特点：**
- 异步处理，不影响主业务
- 支持分布式收集
- 便于集成其他系统
- 需要配置消息队列

**配置：**
```yaml
sql-log:
  audit:
    enabled: true
    push-type: MESSAGE_QUEUE
    message-queue:
      enabled: true
      topic: sql-audit-topic  # Kafka Topic
      exchange: sql-audit-exchange  # RabbitMQ Exchange（可选）
      routing-key: sql.audit  # RabbitMQ Routing Key（可选）
```

#### 4.4 HTTP 推送（HTTP）

通过 HTTP 接口将审计信息推送到外部系统。

**特点：**
- 灵活集成
- 支持自定义处理逻辑
- 可以推送到日志平台
- 支持同步和异步模式

**配置：**
```yaml
sql-log:
  audit:
    enabled: true
    push-type: HTTP
    http:
      enabled: true
      url: http://log-server/api/sql-audit
      timeout: 5000  # 请求超时时间（毫秒）
      async: true    # 是否异步推送（默认 true）
```

**HTTP 请求格式：**
- **方法**：POST
- **Content-Type**：application/json
- **请求体**：`SqlExecutionInfo` 对象的 JSON 序列化

### 5. 日志格式说明

#### 5.1 合并格式（MERGED）

默认格式，SQL 和参数合并显示：

```
2024-01-01 12:00:00 | DS: dataSource | [SUCCESS] | took 10ms | SELECT * FROM user WHERE id = 1
```

**格式说明：**
- `2024-01-01 12:00:00` - 执行时间
- `DS: dataSource` - 数据源名称
- `[SUCCESS]` - 执行结果（SUCCESS/FAILED/SLOW_QUERY）
- `took 10ms` - 执行耗时
- `SELECT * FROM user WHERE id = 1` - 可读 SQL

#### 5.2 官方格式（OFFICIAL）

类似 MyBatis 官方日志格式：

```
==>  Preparing: SELECT * FROM user WHERE id = ?
==> Parameters: 1(Long)
<==    Columns: id, username, email
<==        Row: 1, admin, admin@example.com
<==      Total: 1
```

### 6. 启动 Banner

JBM 框架会在应用启动时显示 SQL 审计配置信息 Banner（可选）。

**配置：**
```yaml
sql-log:
  show-banner: true  # 是否显示启动 Banner（默认 true）
  banner-location: sql-audit-banner.txt  # Banner 文件路径（相对于 classpath）
```

**自定义 Banner：**
在 `classpath` 下创建 `sql-audit-banner.txt` 文件，支持模板变量：
- `${mode}` - 记录模式
- `${slowQuery}` - 慢查询配置
- `${filter}` - 过滤配置
- `${audit}` - 审计配置

## 使用场景

### 场景 1：开发环境调试

在开发环境中，使用普通模式记录所有 SQL，便于调试：

```yaml
sql-log:
  mode: NORMAL
  filter: true
  audit:
    enabled: true
    push-type: LOCAL_LOG
```

### 场景 2：生产环境监控

在生产环境中，使用白名单模式记录关键 SQL，并启用慢查询检测：

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.critical.*
  audit:
    enabled: true
    push-type: LOCAL_LOG
  slow-query:
    enabled: true
    threshold: 3000
```

### 场景 3：性能分析

启用慢查询检测和数据库存储，便于后续性能分析：

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: DATABASE
    database:
      enabled: true
  slow-query:
    enabled: true
    threshold: 2000
```

### 场景 4：集中式日志收集

使用消息队列或 HTTP 推送，实现集中式日志收集：

```yaml
sql-log:
  mode: WHITELIST
  whitelist:
    - com.example.mapper.*
  audit:
    enabled: true
    push-type: MESSAGE_QUEUE
    message-queue:
      enabled: true
      topic: sql-audit-topic
```

## 注意事项

### ⚠️ 重要提示

1. **性能影响**：审计功能会拦截所有 SQL 执行，虽然已优化性能，但在高并发场景下仍可能产生一定开销。建议：
   - 生产环境使用白名单模式，只记录关键 SQL
   - 启用异步推送，避免阻塞主业务
   - 合理设置过滤规则，减少无用日志

2. **日志量控制**：
   - 使用白名单模式限制记录范围
   - 启用 SQL 过滤，过滤无用 SQL
   - 合理设置慢查询阈值
   - 考虑使用日志级别控制

3. **敏感信息**：
   - SQL 审计会记录完整的 SQL 语句和参数
   - 如果包含敏感信息（如密码、密钥），需要谨慎处理
   - 建议在推送前进行脱敏处理

4. **存储空间**：
   - 数据库存储方式会产生大量数据
   - 建议定期清理历史数据
   - 考虑使用分区表或归档策略

5. **网络推送**：
   - HTTP 推送和消息队列推送依赖网络
   - 建议启用异步模式，避免阻塞
   - 配置合理的超时时间
   - 考虑失败重试机制

### 最佳实践

1. **开发环境**：
   - 使用 `NORMAL` 模式，记录所有 SQL
   - 启用详细日志格式
   - 使用本地打印即可

2. **测试环境**：
   - 使用 `WHITELIST` 模式，记录关键 SQL
   - 启用慢查询检测
   - 可以启用数据库存储，便于分析

3. **生产环境**：
   - 使用 `WHITELIST` 模式，严格控制记录范围
   - 启用慢查询检测，阈值设置合理（如 3 秒）
   - 使用异步推送，避免影响主业务
   - 考虑使用消息队列或 HTTP 推送到集中式日志系统

4. **性能优化**：
   - 合理配置白名单，避免记录过多 SQL
   - 启用 SQL 过滤，过滤无用 SQL
   - 使用异步推送方式
   - 定期清理历史审计数据

## 常见问题

### Q1: 如何禁用 SQL 审计功能？

A: 在配置文件中设置 `sql-log.audit.enabled=false` 即可禁用。

### Q2: 慢查询检测是否受白名单限制？

A: 不受限制。慢查询检测会绕过白名单检查，所有超过阈值的 SQL 都会被记录。

### Q3: 如何查看慢查询日志？

A: 慢查询会在日志中明确标识为 `[SLOW_QUERY]`，也可以通过数据库存储方式查询 `slow_query=true` 的记录。

### Q4: 如何自定义日志格式？

A: 使用 `sql-log.custom-format` 配置项，支持多种占位符，详见配置说明。

### Q5: 数据库存储方式需要创建表吗？

A: 需要。框架不会自动创建表，需要手动创建对应的表结构，详见"数据库存储"章节。

### Q6: 如何过滤特定的 SQL？

A: 使用 `sql-log.exclude` 配置项，支持正则表达式，多个规则用 `|` 分隔。

### Q7: 审计功能会影响性能吗？

A: 会有一定性能开销，但已做优化。建议：
- 使用白名单模式限制记录范围
- 启用异步推送
- 合理设置过滤规则

### Q8: 如何同时使用多种推送方式？

A: 设置 `push-type=MULTIPLE`，然后分别启用需要的推送方式（database、message-queue、http）。

### Q9: HTTP 推送失败会重试吗？

A: 当前版本不支持自动重试。如果推送失败，会在日志中记录错误信息，但不会影响 SQL 执行。

### Q10: 如何查看审计配置信息？

A: 应用启动时会显示配置 Banner（如果启用），也可以通过日志查看配置信息。

## 相关文档

- [SqlLogProperties.java](../java/com/jbm/framework/dao/SqlLogProperties.java) - SQL 日志配置类源码
- [SqlInterceptorHandler.java](../java/com/jbm/framework/dao/mybatis/sqlAudit/SqlInterceptorHandler.java) - SQL 拦截处理器源码
- [SqlAuditService.java](../java/com/jbm/framework/dao/mybatis/sqlAudit/SqlAuditService.java) - SQL 审计服务源码
- [MybatisPlusConfig.java](../java/jbm/framework/boot/autoconfigure/mybatis/MybatisPlusConfig.java) - MyBatis Plus 配置类源码
