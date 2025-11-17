package jbm.framework.boot.autoconfigure.mqtt.client;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.jbm.util.FastJsonUtils;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.IMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public class SimpleMqttClient {


    private final MqttProperties mqttProperties;

    private final Mqtt5AsyncClient mqttClient;
    
    // 存储订阅信息以便重连后恢复，key 为 topic，value 为监听器
    private final Map<String, Consumer<Mqtt5Publish>> subscriptions = new ConcurrentHashMap<>();
    
    // 追踪已成功订阅的 topic，用于防止重复订阅（即使监听器相同，也避免重复调用底层订阅）
    private final Set<String> successfullySubscribedTopics = ConcurrentHashMap.newKeySet();
    
    // 追踪正在订阅中的 topic，用于防止并发订阅（在异步订阅完成前防止重复订阅）
    private final Set<String> subscribingTopics = ConcurrentHashMap.newKeySet();
    
    // 用于健康检查的调度器
    private final ScheduledExecutorService healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "mqtt-health-check");
        thread.setDaemon(true);
        return thread;
    });
    
    // 记录上一次的连接状态
    private volatile boolean wasConnected = false;

    public SimpleMqttClient(Mqtt5AsyncClient mqttClient, MqttProperties mqttProperties) {
        this.mqttClient = mqttClient;
        this.mqttProperties = mqttProperties;
        
        // 启动健康检查
        startHealthCheck();
    }

    public MqttClient getClient() {
        return this.mqttClient;
    }


    /**
     * 启动健康检查
     */
    private void startHealthCheck() {
        healthCheckScheduler.scheduleAtFixedRate(() -> {
            try {
                boolean currentlyConnected = isConnected();
                
                // 检测到从断连到连接的状态变化，触发订阅恢复
                if (currentlyConnected && !wasConnected) {
                    if (!subscriptions.isEmpty()) {
                        log.info("🔄 Connection restored, checking if subscription recovery is needed ({} subscriptions)", subscriptions.size());
                        // 检查是否需要恢复订阅（如果所有订阅都已经活跃，就不需要恢复）
                        boolean needRestore = false;
                        for (String topic : subscriptions.keySet()) {
                            if (!successfullySubscribedTopics.contains(topic)) {
                                needRestore = true;
                                break;
                            }
                        }
                        
                        if (!needRestore) {
                            log.debug("🔄 All subscriptions already active, skipping restore");
                            wasConnected = currentlyConnected;
                            return;
                        }
                        
                        log.info("🔄 Some subscriptions need recovery, will restore {} subscriptions after delay", subscriptions.size());
                        // 延迟2秒后恢复订阅，确保连接稳定（弱网环境需要更长时间）
                        healthCheckScheduler.schedule(() -> {
                            try {
                                restoreSubscriptions();
                                log.info("✅ Subscriptions recovery completed");
                            } catch (Exception e) {
                                log.error("❌ Error restoring subscriptions", e);
                                // 失败后再次尝试
                                healthCheckScheduler.schedule(() -> {
                                    try {
                                        restoreSubscriptions();
                                        log.info("✅ Subscriptions recovered on retry");
                                    } catch (Exception ex) {
                                        log.error("❌ Failed to restore subscriptions after retry", ex);
                                    }
                                }, 5, TimeUnit.SECONDS);
                            }
                        }, 2000, TimeUnit.MILLISECONDS);
                    }
                }
                
                // 如果断连，只在 DEBUG 级别记录
                if (!currentlyConnected && wasConnected) {
                    if (log.isDebugEnabled()) {
                        log.debug("⚠️ MQTT Client disconnected - automatic reconnection in progress");
                    }
                }
                
                // 更新连接状态
                wasConnected = currentlyConnected;
            } catch (Exception e) {
                log.error("❌ Error during MQTT health check", e);
            }
        }, 10, 30, TimeUnit.SECONDS); // 10秒后开始，每30秒检查一次（弱网环境减少检查频率）
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return mqttClient.getState().isConnected();
    }
    
    /**
     * 手动触发重连（仅在必要时使用，通常应依赖自动重连机制）
     */
    public void reconnect() {
        if (!isConnected()) {
            log.info("🔄 Manually triggering MQTT reconnection");
            Mqtt5Connect connectMessage = Mqtt5Connect.builder()
                    .keepAlive(mqttProperties.getKeepAliveInterval())
                    .cleanStart(false) // 重连时不清除会话
                    .sessionExpiryInterval(mqttProperties.getSessionExpiryInterval())
                    .build();
            
            mqttClient.connect(connectMessage)
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            log.error("❌ Manual reconnection failed: {}", throwable.getMessage());
                        } else {
                            log.info("✅ Manual reconnection successful");
                            // 重连成功后立即恢复订阅
                            healthCheckScheduler.schedule(() -> {
                                try {
                                    restoreSubscriptions();
                                } catch (Exception e) {
                                    log.error("❌ Error restoring subscriptions after manual reconnect", e);
                                }
                            }, 500, TimeUnit.MILLISECONDS);
                        }
                    });
        }
    }
    
    /**
     * 恢复所有订阅（在重连后调用）
     */
    public void restoreSubscriptions() {
        if (subscriptions.isEmpty()) {
            return;
        }
        
        // 检查是否已经成功订阅过（避免重复订阅）
        // 如果会话没有过期，MQTT 服务器可能已经保存了订阅，不需要重新订阅
        // 但如果会话过期了，需要重新订阅
        boolean needRestore = false;
        for (String topic : subscriptions.keySet()) {
            if (!successfullySubscribedTopics.contains(topic)) {
                needRestore = true;
                break;
            }
        }
        
        if (!needRestore) {
            if (log.isDebugEnabled()) {
                log.debug("🔄 All subscriptions already active, skipping restore");
            }
            return;
        }
        
        // 清空成功订阅标记，因为重连后需要重新订阅
        successfullySubscribedTopics.clear();
        
        log.info("🔄 Restoring {} subscriptions", subscriptions.size());
        
        subscriptions.forEach((topicFilter, messageListener) -> {
            // 如果正在订阅中，跳过（防止并发）
            if (!subscribingTopics.add(topicFilter)) {
                log.warn("⚠️ Topic: {} 正在订阅中，跳过恢复订阅", topicFilter);
                return;
            }
            
            mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback(messageListener)
                    .send()
                    .whenComplete((subAck, throwable) -> {
                        // 无论成功或失败，都从"订阅中"集合中移除
                        subscribingTopics.remove(topicFilter);
                        
                        if (throwable != null) {
                            log.warn("⚠️ Failed to restore subscription for topic {}, will retry", topicFilter);
                            // 订阅失败后重试
                            scheduleRetrySubscription(topicFilter, messageListener, 1);
                        } else {
                            // 恢复订阅成功，标记为已成功订阅
                            successfullySubscribedTopics.add(topicFilter);
                            log.info("✅ Restored subscription: {}", topicFilter);
                        }
                    });
        });
    }
    
    /**
     * 订阅重试机制（增强版，支持长时间重试）
     */
    private void scheduleRetrySubscription(String topicFilter, Consumer<Mqtt5Publish> messageListener, int attempt) {
        // 弱网环境下，允许更多次重试（最多20次）
        if (attempt > 20) {
            log.error("❌ Failed to subscribe to topic {} after {} attempts, will retry on next reconnection", 
                    topicFilter, attempt);
            return;
        }
        
        // 指数退避，最多2分钟（弱网环境需要更长的间隔）
        long delay = Math.min(1000L * (1L << (attempt - 1)), 120000L);
        if (log.isDebugEnabled()) {
            log.debug("⏳ Scheduling retry {} for topic {} in {}s", 
                    attempt, topicFilter, delay/1000);
        }
        
        healthCheckScheduler.schedule(() -> {
            if (!isConnected()) {
                log.warn("⚠️ Cannot retry subscription - client not connected, will retry later");
                scheduleRetrySubscription(topicFilter, messageListener, attempt + 1);
                return;
            }
            
            mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback(messageListener)
                    .send()
                    .whenComplete((subAck, throwable) -> {
                        if (throwable != null) {
                            log.error("❌ Retry {} failed for topic {}: {}", 
                                    attempt, topicFilter, throwable.getMessage());
                            // 重试失败，从成功订阅集合中移除（如果存在）
                            successfullySubscribedTopics.remove(topicFilter);
                            scheduleRetrySubscription(topicFilter, messageListener, attempt + 1);
                        } else {
                            // 重试成功，标记为已成功订阅
                            successfullySubscribedTopics.add(topicFilter);
                            log.info("✅ Retry {} succeeded for topic: {}", attempt, topicFilter);
                        }
                    });
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void subscribe(String topicFilter, Consumer<Mqtt5Publish> messageListener) {
        // 检查是否已经成功订阅过该 topic（防止重复订阅，即使监听器相同）
        if (successfullySubscribedTopics.contains(topicFilter)) {
            Consumer<Mqtt5Publish> existingListener = subscriptions.get(topicFilter);
            // 如果监听器不同，更新监听器引用（虽然不会重新订阅，但更新引用以便后续使用）
            if (existingListener != messageListener) {
                log.warn("⚠️ Topic: {} 已订阅，但监听器不同，更新监听器引用", topicFilter);
                subscriptions.put(topicFilter, messageListener);
            } else {
                log.debug("Topic: {} 已订阅，跳过重复订阅", topicFilter);
            }
            return;
        }
        
        // 检查是否正在订阅中（防止并发订阅）
        if (!subscribingTopics.add(topicFilter)) {
            log.warn("⚠️ Topic: {} 正在订阅中，跳过重复订阅（并发订阅防护）", topicFilter);
            // 如果监听器不同，更新监听器引用（虽然不会重新订阅）
            Consumer<Mqtt5Publish> existingListener = subscriptions.get(topicFilter);
            if (existingListener != messageListener) {
                subscriptions.put(topicFilter, messageListener);
            }
            return;
        }
        
        // 保存订阅信息用于重连恢复
        subscriptions.put(topicFilter, messageListener);
        
        // 订阅主题并设置消息监听器
        mqttClient.subscribeWith()
                .topicFilter(topicFilter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(messageListener)
                .send()
                .whenComplete((subAck, throwable) -> {
                    // 无论成功或失败，都从"订阅中"集合中移除
                    subscribingTopics.remove(topicFilter);
                    
                    if (throwable != null) {
                        log.warn("订阅失败 - Topic: {}, 错误: {}", topicFilter, throwable.getMessage());
                        // 订阅失败时，从成功订阅集合中移除（如果存在）
                        successfullySubscribedTopics.remove(topicFilter);
                        // 订阅失败时启动重试机制
                        scheduleRetrySubscription(topicFilter, messageListener, 1);
                        return;
                    }
                    // 订阅成功，标记为已成功订阅
                    successfullySubscribedTopics.add(topicFilter);
                    log.info("✅ 订阅成功 - Topic: {}", topicFilter);
                });
    }

    public void subscribeWithResponse(String topicFilter, IMqttMessageListener mqttRequestListener) {
        this.subscribe(topicFilter, mqttRequestListener);
    }

    public String sendAndResponse(String requsetTopic, String responseTopic, Object requestMessage) {
        // 使用CountDownLatch来同步等待响应
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> response = new AtomicReference<>();
        this.subscribeWithResponse(responseTopic, new AbstractMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = message.getPayloadStr();
                if (topic.equals(responseTopic)) {
                    // 这里可以添加处理响应的逻辑
                    // 通知主线程可以结束了
                    latch.countDown();
                    response.set(payload);
                    unsubscribe(responseTopic); // 取消订阅响应主题
                } else {
                    log.error("not my response:{}", payload);
                }
            }
        });
        // 构建请求消息
        String requestPayload = null;
        if (requestMessage instanceof String) {
            requestPayload = (String) requestMessage;
        } else {
            requestPayload = JSON.toJSONString(requestMessage);
        }
        if (StrUtil.isBlank(requestPayload)) {
            throw new IllegalArgumentException("requestMessage must not be null");
        }
        MqttMessage mqttMessage = new MqttMessage(requestPayload.getBytes());
        mqttMessage.setQos(1);
        // 发送请求消息
        this.publish(requsetTopic, mqttMessage);
        // 在这里等待响应
        try {
            latch.await(30, TimeUnit.SECONDS);
            return response.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void publish(String topic, MqttMessage message) {
        publish(topic, message, 0);
    }
    
    /**
     * 发布消息，带重试机制
     */
    private void publish(String topic, MqttMessage message, int attempt) {
        if (!isConnected()) {
            log.warn("⚠️ Cannot publish - client not connected, will retry");
            if (attempt < 3) {
                healthCheckScheduler.schedule(() -> publish(topic, message, attempt + 1), 
                        2000L * (attempt + 1), TimeUnit.MILLISECONDS);
            } else {
                log.error("❌ Failed to publish message to topic {} after {} attempts - not connected", 
                        topic, attempt);
            }
            return;
        }
        
        this.mqttClient.publishWith()
                .topic(topic)
                .payload(message.getPayload())
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((publishResult, throwable) -> {
                    if (throwable != null) {
                        log.error("❌ Failed to publish message to topic {}: {}", topic, throwable.getMessage());
                        if (attempt < 3) {
                            log.info("⏳ Retrying publish to topic {} (attempt {})", topic, attempt + 1);
                            healthCheckScheduler.schedule(() -> publish(topic, message, attempt + 1), 
                                    2000L * (attempt + 1), TimeUnit.MILLISECONDS);
                        } else {
                            log.error("❌ Failed to publish message to topic {} after {} attempts", topic, attempt);
                        }
                    } else {
                        if (attempt > 0) {
                            log.info("✅ Successfully published to topic {} after {} retries", topic, attempt);
                        }
                    }
                });
    }

    public void publishObject(String topic, Object message) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(JSON.toJSONBytes(message, FastJsonUtils.defaultWebConfig()));
        mqttMessage.setQos(1);
        this.publish(topic, mqttMessage);
    }


//    public boolean isConnected() {
//        return this.mqttClient.connectWith().
//    }
//
//    public void connect() {
//        if (!this.mqttClient.isConnected()) {
//            this.mqttClient.connect(mqttConnectProperties.toMqttConnectOptions());
//        }
//    }

    public void unsubscribe(String topicFilter) {
        // 从订阅列表中移除
        subscriptions.remove(topicFilter);
        
        this.mqttClient.unsubscribeWith()
                .topicFilter(topicFilter)
                .send()
                .whenComplete((unsuback, throwable) -> {
                    if (throwable != null) {
                        log.error("❌ Failed to unsubscribe from topic {}: {}", topicFilter, throwable.getMessage());
                    } else {
                        log.info("✅ Successfully unsubscribed from topic: {}", topicFilter);
                    }
                });
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        log.info("🛑 Shutting down MQTT client");
        healthCheckScheduler.shutdown();
        try {
            if (!healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
