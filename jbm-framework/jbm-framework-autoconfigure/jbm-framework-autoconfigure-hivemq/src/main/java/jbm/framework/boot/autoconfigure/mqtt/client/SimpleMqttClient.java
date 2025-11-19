package jbm.framework.boot.autoconfigure.mqtt.client;

import com.alibaba.fastjson.JSON;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.jbm.util.FastJsonUtils;
import jbm.framework.boot.autoconfigure.mqtt.IMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import jbm.framework.boot.autoconfigure.mqtt.rpc.MqttRequestResponseManager;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public class SimpleMqttClient {

    /**
     * 可变的监听器包装器，允许在不取消订阅的情况下更新监听器
     */
    private static class MutableListenerWrapper implements Consumer<Mqtt5Publish> {
        private volatile Consumer<Mqtt5Publish> delegate;
        
        public MutableListenerWrapper(Consumer<Mqtt5Publish> initialListener) {
            this.delegate = initialListener;
        }
        
        public void setDelegate(Consumer<Mqtt5Publish> newListener) {
            this.delegate = newListener;
        }
        
        public Consumer<Mqtt5Publish> getDelegate() {
            return delegate;
        }
        
        @Override
        public void accept(Mqtt5Publish publish) {
            Consumer<Mqtt5Publish> current = delegate;
            if (current != null) {
                current.accept(publish);
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MutableListenerWrapper that = (MutableListenerWrapper) obj;
            return delegate == that.delegate;
        }
        
        @Override
        public int hashCode() {
            return delegate != null ? delegate.hashCode() : 0;
        }
    }

    private final MqttProperties mqttProperties;

    private final Mqtt5AsyncClient mqttClient;
    
    // 存储订阅信息以便重连后恢复，key 为 topic，value 为监听器包装器
    private final Map<String, MutableListenerWrapper> subscriptions = new ConcurrentHashMap<>();
    
    // 追踪已成功订阅的 topic，用于防止重复订阅（即使监听器相同，也避免重复调用底层订阅）
    private final Set<String> successfullySubscribedTopics = ConcurrentHashMap.newKeySet();
    
    // 追踪正在订阅中的 topic，用于防止并发订阅（在异步订阅完成前防止重复订阅）
    private final Set<String> subscribingTopics = ConcurrentHashMap.newKeySet();
    
    // 追踪正在取消订阅中的 topic，用于防止在取消订阅过程中重复订阅
    private final Set<String> unsubscribingTopics = ConcurrentHashMap.newKeySet();
    
    // 用于健康检查的调度器
    private final ScheduledExecutorService healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "mqtt-health-check");
        thread.setDaemon(true);
        return thread;
    });
    
    // 记录上一次的连接状态
    private volatile boolean wasConnected = false;
    
    private final MqttRequestResponseManager requestResponseManager;

    public SimpleMqttClient(Mqtt5AsyncClient mqttClient, MqttProperties mqttProperties) {
        this.mqttClient = mqttClient;
        this.mqttProperties = mqttProperties;
        
        // 启动健康检查
        startHealthCheck();

        this.requestResponseManager = new MqttRequestResponseManager(this);
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
        
        subscriptions.forEach((topicFilter, wrapper) -> {
            // 如果正在订阅中，跳过（防止并发）
            if (!subscribingTopics.add(topicFilter)) {
                log.warn("⚠️ Topic: {} 正在订阅中，跳过恢复订阅", topicFilter);
                return;
            }
            
            mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback(wrapper)
                    .send()
                    .whenComplete((subAck, throwable) -> {
                        // 无论成功或失败，都从"订阅中"集合中移除
                        subscribingTopics.remove(topicFilter);
                        
                        if (throwable != null) {
                            log.warn("⚠️ Failed to restore subscription for topic {}, will retry", topicFilter);
                            // 订阅失败后重试
                            scheduleRetrySubscription(topicFilter, wrapper, 1);
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
    private void scheduleRetrySubscription(String topicFilter, MutableListenerWrapper wrapper, int attempt) {
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
                scheduleRetrySubscription(topicFilter, wrapper, attempt + 1);
                return;
            }
            
            mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback(wrapper)
                    .send()
                    .whenComplete((subAck, throwable) -> {
                        if (throwable != null) {
                            log.error("❌ Retry {} failed for topic {}: {}", 
                                    attempt, topicFilter, throwable.getMessage());
                            // 重试失败，从成功订阅集合中移除（如果存在）
                            successfullySubscribedTopics.remove(topicFilter);
                            scheduleRetrySubscription(topicFilter, wrapper, attempt + 1);
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
            MutableListenerWrapper existingWrapper = subscriptions.get(topicFilter);
            if (existingWrapper != null) {
                Consumer<Mqtt5Publish> existingListener = existingWrapper.getDelegate();
                // 如果监听器不同，直接更新包装器内的监听器引用（无需取消订阅）
                if (existingListener != messageListener) {
                    log.debug("⚠️ Topic: {} 已订阅，监听器不同，更新监听器引用（无需取消订阅）", topicFilter);
                    existingWrapper.setDelegate(messageListener);
                    // 同时更新 subscriptions map 中的包装器（虽然引用相同，但确保一致性）
                    subscriptions.put(topicFilter, existingWrapper);
                } else {
                    log.debug("Topic: {} 已订阅，跳过重复订阅", topicFilter);
                }
            } else {
                // 包装器不存在，创建新的包装器并更新
                log.debug("⚠️ Topic: {} 已订阅但包装器丢失，创建新包装器", topicFilter);
                subscriptions.put(topicFilter, new MutableListenerWrapper(messageListener));
            }
            return;
        }
        
        // 检查是否正在取消订阅中（如果正在取消订阅，等待完成后再订阅）
        if (unsubscribingTopics.contains(topicFilter)) {
            log.debug("⚠️ Topic: {} 正在取消订阅中，等待完成后重新订阅", topicFilter);
            // 更新监听器引用，等待取消订阅完成后会重新订阅
            subscriptions.put(topicFilter, new MutableListenerWrapper(messageListener));
            // 延迟后重试订阅
            healthCheckScheduler.schedule(() -> {
                subscribe(topicFilter, messageListener);
            }, 200, TimeUnit.MILLISECONDS);
            return;
        }
        
        // 检查是否正在订阅中（防止并发订阅）
        if (!subscribingTopics.add(topicFilter)) {
            log.warn("⚠️ Topic: {} 正在订阅中，跳过重复订阅（并发订阅防护）", topicFilter);
            // 如果监听器不同，更新监听器引用（当前订阅完成后，下次重连时会使用新监听器）
            MutableListenerWrapper existingWrapper = subscriptions.get(topicFilter);
            if (existingWrapper != null) {
                Consumer<Mqtt5Publish> existingListener = existingWrapper.getDelegate();
                if (existingListener != messageListener) {
                    log.debug("⚠️ Topic: {} 订阅进行中，监听器不同，更新监听器引用（将在订阅完成后生效）", topicFilter);
                    existingWrapper.setDelegate(messageListener);
                }
            } else {
                subscriptions.put(topicFilter, new MutableListenerWrapper(messageListener));
            }
            return;
        }
        
        // 保存订阅信息用于重连恢复（使用包装器）
        MutableListenerWrapper wrapper = new MutableListenerWrapper(messageListener);
        subscriptions.put(topicFilter, wrapper);
        
        // 订阅主题并设置消息监听器（使用包装器，这样即使监听器更新，包装器引用不变）
        mqttClient.subscribeWith()
                .topicFilter(topicFilter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(wrapper)
                .send()
                .whenComplete((subAck, throwable) -> {
                    // 无论成功或失败，都从"订阅中"集合中移除
                    subscribingTopics.remove(topicFilter);
                    
                    if (throwable != null) {
                        log.warn("订阅失败 - Topic: {}, 错误: {}", topicFilter, throwable.getMessage());
                        // 订阅失败时，从成功订阅集合中移除（如果存在）
                        successfullySubscribedTopics.remove(topicFilter);
                        // 订阅失败时启动重试机制
                        scheduleRetrySubscription(topicFilter, wrapper, 1);
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

    /**
     * 发送请求并等待响应（优化版，支持并发和复用订阅）
     * 
     * @param requestTopic 请求 topic
     * @param responseTopic 响应 topic
     * @param requestMessage 请求消息
     * @return 响应消息
     */
    public String sendAndResponse(String requestTopic, String responseTopic, Object requestMessage) {
        return sendAndResponse(requestTopic, responseTopic, requestMessage, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 发送请求并等待响应（优化版，支持并发和复用订阅）
     * 
     * @param requestTopic 请求 topic
     * @param responseTopic 响应 topic
     * @param requestMessage 请求消息
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 响应消息
     */
    public String sendAndResponse(String requestTopic, String responseTopic, Object requestMessage, 
                                   long timeout, TimeUnit unit) {
        return requestResponseManager.send(requestTopic, responseTopic, requestMessage, timeout, unit);
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
        // 从成功订阅集合中移除
        successfullySubscribedTopics.remove(topicFilter);
        // 从订阅中集合中移除（如果存在）
        subscribingTopics.remove(topicFilter);
        // 标记为正在取消订阅
        unsubscribingTopics.add(topicFilter);
        
        this.mqttClient.unsubscribeWith()
                .topicFilter(topicFilter)
                .send()
                .whenComplete((unsuback, throwable) -> {
                    // 从取消订阅集合中移除
                    unsubscribingTopics.remove(topicFilter);
                    
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
        
        requestResponseManager.shutdown();
        
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
