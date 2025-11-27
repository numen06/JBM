# 业务日志集成模块

## 📦 模块说明

本模块为 `BusinessLogController` 提供了完整的集成方案，让其他项目可以通过 **Feign 客户端**或 **RabbitMQ 消息队列**两种方式来使用业务日志功能。

## 📁 文件清单

### 1. 核心模板类
- **`JbmBusinessLogTemplate.java`** - 业务日志模板类，提供便捷的日志操作方法（RabbitMQ方式）

### 2. API 模型类
位于 `jbm-cluster-api/jbm-cluster-api-basic/src/main/java/com/jbm/cluster/api/model/log/`

- **`BusinessLogEvent.java`** - 业务日志事件对象
- **`BusinessLogEventType.java`** - 事件类型枚举（CREATE、APPEND、DELETE等）
- **`BusinessLogRequest.java`** - Feign调用请求对象
- **`BusinessLogResponse.java`** - Feign调用响应对象

### 3. Feign 客户端
- **`BusinessLogClient.java`** - Feign客户端接口（位于 `jbm-cluster-api-basic`）

### 4. 消息队列消费者
- **`BusinessLogEventListener.java`** - 消息队列事件监听器（位于 `jbm-cluster-platform-logs`）

### 5. 常量定义
- **`QueueConstants.java`** - 新增 `BUSINESS_LOG_STREAM` 常量（位于 `jbm-cluster-core`）

### 6. 控制器增强
- **`BusinessLogController.java`** - 新增 Feign API 接口（位于 `jbm-cluster-platform-logs`）

### 7. 文档
- **`BUSINESS_LOG_INTEGRATION.md`** - 完整的集成使用指南
- **`README_BUSINESS_LOG.md`** - 本文件

## 🚀 快速开始

### 方式一：RabbitMQ 异步推送（推荐）

#### 1. 添加依赖
```xml
<dependency>
    <groupId>com.jbm.cluster</groupId>
    <artifactId>jbm-cluster-common-basic</artifactId>
</dependency>
```

#### 2. 注册Bean
```java
@Configuration
public class BusinessLogConfig {
    @Bean
    public JbmBusinessLogTemplate jbmBusinessLogTemplate() {
        return new JbmBusinessLogTemplate();
    }
}
```

#### 3. 使用示例
```java
@Service
public class OrderService {
    @Autowired
    private JbmBusinessLogTemplate businessLogTemplate;
    
    public void createOrder(Order order) {
        // 记录业务日志
        businessLogTemplate.createLog(
            "ORDER",              // 业务类型
            order.getOrderNo(),   // 业务ID
            "订单创建成功",       // 日志内容
            "order-service"       // 日志来源
        );
    }
}
```

### 方式二：Feign 同步调用

#### 1. 添加依赖
```xml
<dependency>
    <groupId>com.jbm.cluster</groupId>
    <artifactId>jbm-cluster-api-basic</artifactId>
</dependency>
```

#### 2. 启用Feign
```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.jbm.cluster.api.client")
public class Application {
    // ...
}
```

#### 3. 使用示例
```java
@Service
public class PaymentService {
    @Autowired
    private BusinessLogClient businessLogClient;
    
    public void processPayment(String paymentNo) {
        // 创建日志并获取logId
        BusinessLogRequest request = BusinessLogRequest.builder()
            .businessType("PAYMENT")
            .businessId(paymentNo)
            .content("支付处理开始")
            .source("payment-service")
            .build();
        
        ResultBody<Map<String, String>> result = 
            businessLogClient.createLog(request);
        String logId = result.getData().get("logId");
        
        // 追加日志
        businessLogClient.appendLog(logId, "支付成功");
    }
}
```

## 📊 功能特性

### JbmBusinessLogTemplate 提供的方法

| 方法 | 说明 | 适用场景 |
|-----|-----|---------|
| `createLog()` | 创建业务日志 | 业务流程开始 |
| `appendLog()` | 追加日志内容 | 记录流程步骤 |
| `appendLogByBusinessId()` | 通过业务ID追加日志 | 无logId场景 |
| `deleteLog()` | 删除日志 | 清理日志 |
| `updateExpireTime()` | 更新过期时间 | 延长保存期 |
| `recordOperation()` | 记录操作日志 | 用户操作记录 |
| `recordError()` | 记录错误日志 | 异常处理 |
| `recordStep()` | 记录步骤日志 | 多步骤流程 |
| `recordBatch()` | 批量记录日志 | 批处理场景 |

### BusinessLogClient 提供的接口

| 接口 | 说明 | 返回值 |
|-----|-----|--------|
| `createLog()` | 创建日志 | logId |
| `appendLog()` | 追加日志 | Boolean |
| `getLog()` | 查询日志 | BusinessLogResponse |
| `getLogFullContent()` | 获取完整内容 | String |
| `deleteLog()` | 删除日志 | Boolean |
| `generateTemporaryUrl()` | 生成临时URL | URL信息 |
| `getLogTotalLines()` | 获取总行数 | Integer |

## ⚙️ 配置说明

### 生产者配置（业务服务）
```yaml
spring:
  cloud:
    stream:
      bindings:
        businessLog-out-0:
          destination: cloud.business.log
```

### 消费者配置（日志服务）
```yaml
spring:
  cloud:
    stream:
      bindings:
        businessLog-in-0:
          destination: cloud.business.log
          group: business-log-group
      function:
        definition: businessLog
```

## 🔍 使用建议

1. **日志记录** → 使用 **RabbitMQ方式**（异步，不阻塞业务）
2. **日志查询** → 使用 **Feign方式**（同步，立即获取结果）
3. **混合使用** → 记录用MQ，查询用Feign

## 📖 详细文档

完整的使用指南请查看：[BUSINESS_LOG_INTEGRATION.md](./BUSINESS_LOG_INTEGRATION.md)

## 🏗️ 架构图

```
┌─────────────────┐
│  业务服务        │
│  (Order/Pay)    │
└────────┬────────┘
         │
         ├─────────────────┬─────────────────┐
         │                 │                 │
         │ RabbitMQ        │ Feign           │
         │ (异步推送)      │ (同步调用)      │
         │                 │                 │
         ▼                 ▼                 │
┌─────────────────┐  ┌──────────────────┐  │
│  消息队列        │  │  BusinessLog     │◄─┘
│  (RabbitMQ)     │  │  Controller      │
└────────┬────────┘  └──────────────────┘
         │                    │
         ▼                    │
┌─────────────────┐           │
│  消费者监听器    │           │
│  Listener       │           │
└────────┬────────┘           │
         │                    │
         └──────┬─────────────┘
                │
                ▼
        ┌───────────────┐
        │  BusinessLog  │
        │  Service      │
        └───────┬───────┘
                │
                ▼
        ┌───────────────┐
        │  OpenObserve  │
        │  (日志存储)   │
        └───────────────┘
```

## ❓ 常见问题

**Q: RabbitMQ方式能获取logId吗？**  
A: 不能直接获取。建议使用 businessType+businessId 作为唯一标识，或使用Feign方式查询。

**Q: 日志会丢失吗？**  
A: 配置消息持久化后不会丢失。详见配置说明。

**Q: 过期时间如何选择？**  
A: 根据业务重要性选择：7天（临时）、30天（普通）、90天（重要）、365天（审计）。

## 📝 更新日志

### v1.0.0 (2025-01-11)
- ✅ 创建 JbmBusinessLogTemplate 模板类
- ✅ 创建 BusinessLogClient Feign客户端
- ✅ 创建消息队列事件模型和监听器
- ✅ 扩展 BusinessLogController 支持 Feign API
- ✅ 更新 QueueConstants 添加队列常量
- ✅ 编写完整的集成文档

## 👥 维护者

@wesley

## 📄 许可证

内部项目，仅供公司内部使用。

