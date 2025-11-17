# MQTT 客户端优化 - 使用指南

## 🚨 当前问题诊断

根据您的日志：
```
❌ Disconnected, Reconnect attempts:0, Reason:Server sent DISCONNECT
✅ Connected
❌ Disconnected, Reconnect attempts:0, Reason:Server sent DISCONNECT
ClientId: FEIGE-MQTT_f245445eb3d8461e9e5f34f88e81fb22 (已有UUID)
```

**诊断结果**：🚨 **应用内部重复创建客户端！**

### 真正的原因
Client ID 后面已经有 UUID（如 `_f245445eb3d8461e9e5f34f88e81fb22`），说明唯一性是有的。但仍然出现冲突，说明：

**问题根源**：**同一个应用内部，多次调用 `getAppClientInstance()`，每次都创建新客户端，虽然传入相同的参数，但因为没有缓存，所以创建了多个相同 Client ID 的连接！**

例如：
```java
// 第1次调用 - 创建客户端1，Client ID: FEIGE-MQTT_xxx
SimpleMqttClient client1 = factory.getAppClientInstance("FEIGE-MQTT");

// 第2次调用 - 又创建客户端2，相同的 Client ID: FEIGE-MQTT_xxx
SimpleMqttClient client2 = factory.getAppClientInstance("FEIGE-MQTT");

// 结果：客户端1被客户端2踢出！
```

## ✅ 解决方案（已自动修复）

### 核心修复：在 `RealMqttPahoClientFactory` 中添加客户端缓存

**修复前**：
```java
public SimpleMqttClient getAppClientInstance(String clientId, Object... tags) {
    // 每次都创建新客户端 ❌
    return new SimpleMqttClient(mqtt5AsyncClient, properties);
}
```

**修复后**：
```java
public SimpleMqttClient getAppClientInstance(String clientId, Object... tags) {
    // 使用缓存，相同 Client ID 返回同一个实例 ✅
    return clientCache.computeIfAbsent(fullClientId, id -> {
        log.info("🔌 Creating new MQTT client: ClientId={}", id);
        // 创建新客户端
        return new SimpleMqttClient(mqtt5AsyncClient, properties);
    });
}
```

### 效果
```java
// 第1次调用 - 创建客户端，Client ID: FEIGE-MQTT_xxx
SimpleMqttClient client1 = factory.getAppClientInstance("FEIGE-MQTT");
// 日志：🔌 Creating new MQTT client: ClientId=FEIGE-MQTT_xxx

// 第2次调用 - 返回缓存的客户端（不创建新的）
SimpleMqttClient client2 = factory.getAppClientInstance("FEIGE-MQTT");
// 无日志（使用缓存）

// client1 == client2 ✅ 是同一个实例！
```

### 2. 弱网环境配置（支持长时间断连）

```properties
spring.mqtt.url=tcp://your-broker:1883
spring.mqtt.automatic-reconnect=true
spring.mqtt.mqtt-version=5

# 弱网环境优化（支持断连1天）
spring.mqtt.clean-start=false
spring.mqtt.session-expiry-interval=86400
spring.mqtt.keep-alive-interval=60
spring.mqtt.max-reconnect-delay=120
spring.mqtt.connection-timeout=15s
```

## 📊 优化后的日志说明

### 正常运行日志（修复后）
```
🔌 Creating new MQTT client: ClientId=FEIGE-MQTT_f245445eb3d8461e9e5f34f88e81fb22
✅ MQTT client connected successfully: ClientId=FEIGE-MQTT_f245445eb3d8461e9e5f34f88e81fb22
✅ Subscribed to topic: /test/from
✅ Subscribed to topic: /test/to

（多次调用 getAppClientInstance 不会再创建新客户端）
```

### Client ID 冲突告警（修复后不应再出现）
```
🚨 CRITICAL: Client ID conflict detected! ClientId:xxx, Reason:Server sent DISCONNECT
🚨 Another system is using the same Client ID. Please ensure Client ID is unique!

（修复后，应用内部不会再重复创建，此告警不应出现）
```

### 正常网络断连重连
```
🔄 Disconnected (Reconnect attempts:1), will auto reconnect
🔄 Disconnected (Reconnect attempts:2), will auto reconnect
🔄 Connection restored, recovering 5 subscriptions
✅ 5 subscriptions recovered successfully
```

**注意**：
- 日志已优化，减少冗余信息
- INFO 级别：关键操作和告警
- DEBUG 级别：详细诊断信息

## 🔍 如何排查 Client ID 冲突

### 方法一：查看启动日志
```
✅ MQTT client connected successfully: ClientId=FEIGE-MQTT_cf5624bc23674b5687c7a99ca6d8e5b2
```

如果看到相同的 Client ID 在多个系统日志中出现，说明有冲突。

### 方法二：查看 MQTT Broker 端

```bash
# Mosquitto
mosquitto_sub -h broker-host -t '$SYS/broker/clients/active' -v

# HiveMQ
# 登录 HiveMQ 控制台查看 Clients 列表
```

### 方法三：启用 DEBUG 日志
```properties
logging.level.jbm.framework.boot.autoconfigure.mqtt=DEBUG
```

## 🎯 优化效果

| 项目 | 优化前 | 优化后 |
|------|--------|--------|
| **Client ID** | 固定值，多系统冲突 | 唯一值，避免冲突 |
| **客户端缓存** | 每个Mapper创建客户端 | 相同ID共享客户端 |
| **订阅恢复** | 手动 | 自动（重连后） |
| **弱网支持** | 断连1小时会话过期 | 支持断连24小时 |
| **日志输出** | 冗余信息多 | 简洁清晰 |
| **故障诊断** | 难以定位 | 自动检测告警 |

## 📋 核心优化清单

- ✅ **客户端缓存（双重缓存）**：
  - `MqttProxyFactory` 缓存：相同 Client ID 的 `@MqttMapper` 共享客户端
  - `RealMqttPahoClientFactory` 缓存：相同 Client ID 的 `getAppClientInstance()` 调用共享客户端
- ✅ **Client ID 唯一性**：自动添加 UUID 后缀（`_xxx`）
- ✅ **自动订阅恢复**：重连后自动恢复所有订阅
- ✅ **弱网环境支持**：
  - Keep-Alive: 60秒（避免频繁超时）
  - Session 有效期: 24小时（支持长时间断连）
  - 重连延迟: 最多120秒（持续尝试）
  - 订阅重试: 20次（更多容错）
- ✅ **智能日志**：
  - 自动检测 Client ID 冲突并告警
  - 减少冗余日志
  - DEBUG 级别查看详细信息
- ✅ **健康检查**：每30秒检查连接状态

## 🚀 快速开始

### 1. 无需修改代码！缓存已自动启用

您的代码可以保持不变：
```java
// 多次调用同一个 Client ID，会自动使用缓存
SimpleMqttClient client = deviceMqttPahoClientFactory
    .getAppClientInstance(device.getMessageProtocolCode());
```

### 2. 重启应用

### 3. 验证日志

**第一次创建客户端**：
```
🔌 Creating new MQTT client: ClientId=FEIGE-MQTT_xxx
✅ MQTT client connected successfully: ClientId=FEIGE-MQTT_xxx
```

**后续调用（使用缓存）**：
```
（不会有创建日志，直接使用缓存的客户端）
```

**不应该再看到**：
```
🚨 CRITICAL: Client ID conflict detected!  ← 不应该出现
❌ Disconnected, Reconnect attempts:0  ← 不应该频繁出现
```

## 🛠️ 故障排查

### Q: 仍然频繁断连？
**A**: 检查：
1. Client ID 是否真的唯一（查看日志中的完整 Client ID）
2. 是否有其他系统在使用
3. 在 Broker 端查看活跃连接列表

### Q: 断连后订阅没有恢复？
**A**: 
1. 检查 `clean-start=false`
2. 检查 `session-expiry-interval` 是否够长
3. 查看日志中是否有 "Connection restored, recovering X subscriptions"

### Q: 日志太多？
**A**: 设置日志级别：
```properties
logging.level.jbm.framework.boot.autoconfigure.mqtt=INFO
```

### Q: 需要更多诊断信息？
**A**: 启用 DEBUG：
```properties
logging.level.jbm.framework.boot.autoconfigure.mqtt=DEBUG
```

## 📞 技术支持

如果问题持续存在，请提供：
1. 完整的启动日志
2. Client ID（从日志中提取）
3. 断连日志（包含 Reason 字段）
4. 配置文件内容

## 🎉 总结

经过优化，您的 MQTT 客户端现在支持：
- ✅ 多系统并发使用（Client ID 唯一）
- ✅ 极端弱网环境（断连24小时仍可恢复）
- ✅ 自动故障恢复（订阅自动恢复）
- ✅ 智能故障诊断（自动检测 Client ID 冲突）
- ✅ 简洁日志输出（减少噪音）

**最关键的配置**：确保 Client ID 唯一！
```properties
spring.mqtt.client-id=${spring.application.name}-${random.uuid}
```

