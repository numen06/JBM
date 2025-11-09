# Webhook 业务事件推送说明

## 概述

业务事件推送系统用于在集群内的各个服务之间传递业务事件。当业务服务（如WCS服务）发生特定事件时，会通过消息队列推送到 Push 服务，Push 服务再通过 Webhook 方式转发到配置的目标地址。

## 工作流程

```
业务服务 → 发送事件 → MQ → Push服务 → Webhook推送 → 目标服务
   ↓                              ↓
 注册事件                    查询配置并推送
```

### 1. 事件注册流程

业务服务启动时，会自动扫描带有 `@BusinessEvent` 注解的类，并将配置注册到 Push 服务。

**业务服务端配置：**

```java
@BusinessEvent(
    name = "任务异常事件",
    group = "wcs-group",           // 事件分组（必须）
    url = "http://target-service/webhook/task-exception",
    methodType = "POST"
)
public class TaskSendExceptionEvent {
    private String taskId;
    private String errorMessage;
    // ... 其他字段
}
```

**日志输出：**
```
📝 接收到事件注册请求，服务ID: wcs-service, 事件数量: 10
   🔧 注册事件: com.jajachina.wcs.event.business.TaskSendExceptionEvent
      ✅ 配置保存成功
```

### 2. 事件推送流程

业务服务发送事件后，Push 服务会：
1. 接收事件消息
2. 查询数据库中的配置
3. 根据配置执行 Webhook 推送

**日志输出：**
```
📥 接收到业务事件推送: com.jajachina.wcs.event.business.TaskSendExceptionEvent
🔍 开始查询业务事件配置
📋 业务事件 [xxx] 查询到 2 个配置
   └─ 启用: 1 个, 禁用: 1 个
✅ 找到 1 个可用的发送配置
```

### 3. 没有配置的情况（优雅处理）

如果查询不到可用的配置，系统会：
- ⚠️ 记录警告日志
- ⏭️ 跳过推送
- ✅ 不会抛出异常中断流程

**日志输出：**
```
⚠️  不存在可用的发送配置，业务事件代码: xxx
💡 可能原因: 1) 配置尚未注册 2) 配置已禁用 3) 目标服务未上线
📝 建议: 如需推送该事件，请在目标服务中添加 @BusinessEvent 注解并重启服务
⏭️  跳过事件推送，因为没有可用的配置
```

## 常见问题

### Q1: 为什么没有配置？

**可能原因：**
1. **目标服务未启动或未注册**
   - 解决方案：启动目标服务，确保带有 `@BusinessEvent` 注解

2. **配置被禁用**
   - 解决方案：在数据库中将 `enable` 字段设为 `1`
   ```sql
   UPDATE webhook_event_config 
   SET enable = 1 
   WHERE business_event_code = 'xxx';
   ```

3. **事件分组(eventGroup)未设置**
   - 解决方案：在 `@BusinessEvent` 注解中添加 `group` 属性

### Q2: 配置存在但没有推送？

**检查要点：**
1. 配置是否启用（`enable = 1`）
2. 事件分组是否设置（`event_group` 不为空）
3. URL 是否正确且可访问

**查询 SQL：**
```sql
SELECT * FROM webhook_event_config 
WHERE business_event_code = '你的事件代码'
  AND enable = 1;
```

### Q3: 如何手动添加配置？

如果自动注册失败，可以手动插入：

```sql
INSERT INTO webhook_event_config (
    event_id, business_event_code, event_name, event_group,
    service_name, enable, url, method_type, create_time, update_time
) VALUES (
    UUID(),
    'com.jajachina.wcs.event.business.TaskSendExceptionEvent',
    '任务异常事件',
    'wcs-group',              -- 必须设置分组
    'wcs-service',
    1,                         -- 启用
    'http://target-service/webhook/task-exception',
    'POST',
    NOW(), NOW()
);
```

## 日志级别说明

### INFO 级别
- 事件接收、推送成功/失败的关键信息
- 配置查询结果统计

### WARN 级别
- 没有可用配置（这是正常情况，不是错误）
- 配置未启用提醒

### DEBUG 级别
- 详细的事件内容
- 配置详情
- 转换过程

### ERROR 级别
- 系统异常（数据库、网络等）
- 关键字段缺失

## 调试建议

### 1. 开启 DEBUG 日志

在 `application.yml` 中：
```yaml
logging:
  level:
    com.jbm.cluster.push: DEBUG
```

### 2. 查看完整流程日志

```bash
# 查看事件接收
tail -f logs/jbm-cluster-platform-push/console.log | grep "接收到业务事件"

# 查看配置查询
tail -f logs/jbm-cluster-platform-push/console.log | grep "查询业务事件配置"

# 查看推送结果
tail -f logs/jbm-cluster-platform-push/console.log | grep "推送"
```

### 3. 使用诊断 SQL

执行 `webhook_diagnostic.sql` 文件中的查询语句，快速了解配置状态。

## 配置表结构

**webhook_event_config 表：**

| 字段                | 说明           | 是否必填 |
|---------------------|----------------|----------|
| event_id            | 事件唯一ID     | 是       |
| business_event_code | 业务事件代码   | 是       |
| event_name          | 事件名称       | 否       |
| event_group         | 事件分组       | **是**   |
| service_name        | 服务名称       | 否       |
| enable              | 是否启用       | 是       |
| url                 | 推送URL        | 是       |
| method_type         | 请求方法       | 是       |

⚠️ **重要**：`event_group` 必须设置，否则无法进行推送！

## 性能优化建议

1. **合理设置日志级别**
   - 生产环境建议使用 INFO 级别
   - 排查问题时临时开启 DEBUG

2. **定期清理历史数据**
   ```sql
   DELETE FROM webhook_task 
   WHERE create_time < DATE_SUB(NOW(), INTERVAL 2 MONTH);
   ```

3. **监控配置数量**
   ```sql
   SELECT service_name, COUNT(*) 
   FROM webhook_event_config 
   GROUP BY service_name;
   ```

## 联系支持

如有问题，请提供：
1. 完整的错误日志（包括堆栈信息）
2. 事件代码（business_event_code）
3. 数据库配置查询结果
4. 业务服务启动日志

---

**版本**: 2.0  
**更新日期**: 2025-11-10  
**维护者**: wesley.zhang

