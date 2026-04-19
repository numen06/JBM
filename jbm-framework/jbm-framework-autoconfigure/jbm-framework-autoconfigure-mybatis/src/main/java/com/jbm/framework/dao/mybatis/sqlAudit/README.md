# SQL 审计模块使用说明

## 概述

SQL 审计模块提供了多种推送方式，用于将 SQL 执行信息推送到不同的目标（本地日志、数据库、消息队列、HTTP 接口等）。

## 配置说明

### 基础配置

```yaml
sql-log:
  # SQL日志白名单，匹配的mapper方法会输出日志
  whitelist:
    - com.jbm.**.mapper.*
  
  # 日志格式类型：MERGED（合并格式）或 OFFICIAL（官方格式）
  format: MERGED
  
  # 审计配置
  audit:
    # 是否启用审计（默认 true）
    enabled: true
    
    # 审计推送方式（默认 LOCAL_LOG）
    # 可选值：LOCAL_LOG（本地打印）、DATABASE（数据库）、MESSAGE_QUEUE（消息队列）、HTTP（HTTP推送）、MULTIPLE（多种方式组合）
    push-type: LOCAL_LOG
    
    # 是否启用本地打印（默认 true，即使使用其他推送方式也会保留本地打印）
    enable-local-log: true
    
    # 数据库推送配置
    database:
      enabled: false
      table-name: sql_audit_log  # 可选，如果为空则使用默认表名
    
    # 消息队列推送配置
    message-queue:
      enabled: false
      topic: sql-audit-topic      # Kafka Topic 或 RabbitMQ Exchange
      exchange: sql-audit-exchange # RabbitMQ Exchange（可选）
      routing-key: sql.audit       # RabbitMQ Routing Key（可选）
    
    # HTTP 推送配置
    http:
      enabled: false
      url: http://localhost:8080/api/sql-audit
      timeout: 5000                # 请求超时时间（毫秒）
      async: true                  # 是否异步推送
```

## 使用示例

### 1. 仅本地打印（默认方式）

```yaml
sql-log:
  whitelist:
    - com.jbm.**.mapper.*
  audit:
    enabled: true
    push-type: LOCAL_LOG
    enable-local-log: true
```

### 2. 数据库存储

```yaml
sql-log:
  whitelist:
    - com.jbm.**.mapper.*
  audit:
    enabled: true
    push-type: DATABASE
    enable-local-log: true  # 保留本地打印
    database:
      enabled: true
      table-name: sql_audit_log
```

### 3. 消息队列推送

```yaml
sql-log:
  whitelist:
    - com.jbm.**.mapper.*
  audit:
    enabled: true
    push-type: MESSAGE_QUEUE
    enable-local-log: true
    message-queue:
      enabled: true
      topic: sql-audit-topic
      exchange: sql-audit-exchange
      routing-key: sql.audit
```

### 4. HTTP 推送

```yaml
sql-log:
  whitelist:
    - com.jbm.**.mapper.*
  audit:
    enabled: true
    push-type: HTTP
    enable-local-log: true
    http:
      enabled: true
      url: http://localhost:8080/api/sql-audit
      timeout: 5000
      async: true
```

### 5. 多种方式组合

```yaml
sql-log:
  whitelist:
    - com.jbm.**.mapper.*
  audit:
    enabled: true
    push-type: MULTIPLE
    enable-local-log: true
    database:
      enabled: true
      table-name: sql_audit_log
    message-queue:
      enabled: true
      topic: sql-audit-topic
    http:
      enabled: true
      url: http://localhost:8080/api/sql-audit
      async: true
```

## 扩展开发

### 实现自定义推送处理器

1. 实现 `SqlAuditPushHandler` 接口：

```java
public class CustomPushHandler implements SqlAuditPushHandler {
    @Override
    public SqlAuditPushType getPushType() {
        return SqlAuditPushType.CUSTOM; // 需要先添加新的枚举值
    }
    
    @Override
    public void push(SqlExecutionInfo executionInfo) {
        // 实现推送逻辑
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

2. 在 `SqlAuditService` 中注册自定义处理器。

## 注意事项

1. **性能影响**：启用审计功能会对 SQL 执行性能产生一定影响，建议在生产环境中使用异步推送方式。

2. **数据安全**：SQL 审计信息可能包含敏感数据，请确保推送目标的安全性。

3. **资源管理**：使用异步推送时，系统会创建线程池，请确保合理配置线程池大小。

4. **错误处理**：推送失败不会影响 SQL 执行，错误信息会记录到日志中。
