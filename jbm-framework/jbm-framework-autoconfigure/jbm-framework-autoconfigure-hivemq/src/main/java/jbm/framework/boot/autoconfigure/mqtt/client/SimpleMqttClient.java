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
    
    // 首次健康检查不触发 restore，避免将「首次连接」误判为「重连」
    private volatile boolean hasCompletedFirstHealthCheck = false;
    
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
                
                // 首次健康检查仅更新状态，不触发 restore（首次连接 ≠ 重连）
                if (!hasCompletedFirstHealthCheck) {
                    hasCompletedFirstHealthCheck = true;
                    wasConnected = currentlyConnected;
                    return;
                }
                // 检测到从断连到连接的状态变化，触发订阅恢复
                if (currentlyConnected && !wasConnected) {
                    if (!subscriptions.isEmpty()) {
                        // 重连后 HiveMQ 客户端的 publish flow 会丢失，必须清空成功标记并强制重新订阅
                        successfullySubscribedTopics.clear();
                        log.info("🔄 Connection restored, restoring {} subscriptions to avoid 'No publish flow registered'", subscriptions.size());
                        // 尽量立即恢复订阅（100ms），减少重连后消息先于 flow 到达导致的 "No publish flow registered" 窗口
                        healthCheckScheduler.schedule(() -> {
                            try {
                                restoreSubscriptions();
                                log.info("✅ Subscriptions recovery completed");
                            } catch (Exception e) {
                                log.error("❌ Error restoring subscriptions", e);
                                healthCheckScheduler.schedule(() -> {
                                    try {
                                        restoreSubscriptions();
                                        log.info("✅ Subscriptions recovered on retry");
                                    } catch (Exception ex) {
                                        log.error("❌ Failed to restore subscriptions after retry", ex);
                                    }
                                }, 5, TimeUnit.SECONDS);
                            }
                        }, 100, TimeUnit.MILLISECONDS);
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
        // 重连后 HiveMQ 的 publish flow 会丢失，必须清空并重新订阅；MqttConnectedEvent 触发时同样需要
        successfullySubscribedTopics.clear();
        log.info("🔄 Restoring {} subscriptions", subscriptions.size());
        subscriptions.forEach((topicFilter, wrapper) -> doSubscribe(topicFilter, wrapper, null));
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
        subscribeInternal(topicFilter, messageListener, null);
    }

    /**
     * 订阅主题并阻塞直到收到 SUBACK 或超时，确保 publish flow 已注册后再发布消息
     *
     * @param topicFilter 主题过滤
     * @param messageListener 消息回调
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 订阅是否成功完成
     */
    public boolean subscribeAndWait(String topicFilter, Consumer<Mqtt5Publish> messageListener,
                                    long timeout, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        subscribeInternal(topicFilter, messageListener, future);
        try {
            future.get(timeout, unit);
            return true;
        } catch (TimeoutException e) {
            log.warn("⏳ 订阅超时 - Topic: {}", topicFilter);
            return false;
        } catch (Exception e) {
            log.warn("❌ 订阅异常 - Topic: {}, 错误: {}", topicFilter, e.getMessage());
            return false;
        }
    }

    private void subscribeInternal(String topicFilter, Consumer<Mqtt5Publish> messageListener,
                                   CompletableFuture<Void> onComplete) {
        if (topicFilter == null || topicFilter.isEmpty()) {
            throw new IllegalArgumentException("topicFilter must not be null or empty");
        }
        if (messageListener == null) {
            throw new IllegalArgumentException("messageListener must not be null");
        }
        if (successfullySubscribedTopics.contains(topicFilter)) {
            MutableListenerWrapper existingWrapper = subscriptions.get(topicFilter);
            if (existingWrapper != null && existingWrapper.getDelegate() != messageListener) {
                existingWrapper.setDelegate(messageListener);
            } else if (existingWrapper == null) {
                subscriptions.put(topicFilter, new MutableListenerWrapper(messageListener));
            }
            if (onComplete != null) onComplete.complete(null);
            return;
        }
        if (unsubscribingTopics.contains(topicFilter)) {
            subscriptions.put(topicFilter, new MutableListenerWrapper(messageListener));
            healthCheckScheduler.schedule(() -> subscribeInternal(topicFilter, messageListener, onComplete),
                    200, TimeUnit.MILLISECONDS);
            return;
        }
        MutableListenerWrapper wrapper = new MutableListenerWrapper(messageListener);
        subscriptions.put(topicFilter, wrapper);
        if (!isConnected()) {
            subscribingTopics.remove(topicFilter);
            if (onComplete != null) onComplete.completeExceptionally(new IllegalStateException("Client not connected"));
            return;
        }
        doSubscribe(topicFilter, wrapper, onComplete);
    }

    /**
     * 执行实际的 MQTT 订阅
     */
    private void doSubscribe(String topicFilter, MutableListenerWrapper wrapper,
                             CompletableFuture<Void> onComplete) {
        if (!subscribingTopics.add(topicFilter)) {
            log.debug("Topic: {} already subscribing, skip", topicFilter);
            if (onComplete != null) onComplete.complete(null);
            return;
        }
        mqttClient.subscribeWith()
                .topicFilter(topicFilter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(wrapper)
                .send()
                .whenComplete((subAck, throwable) -> {
                    subscribingTopics.remove(topicFilter);
                    if (throwable != null) {
                        log.warn("订阅失败 - Topic: {}, 错误: {}", topicFilter, throwable.getMessage());
                        successfullySubscribedTopics.remove(topicFilter);
                        scheduleRetrySubscription(topicFilter, wrapper, 1);
                        if (onComplete != null) onComplete.completeExceptionally(throwable);
                        return;
                    }
                    successfullySubscribedTopics.add(topicFilter);
                    log.info("✅ 订阅成功 - Topic: {}", topicFilter);
                    if (onComplete != null) onComplete.complete(null);
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
