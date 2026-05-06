package jbm.framework.boot.autoconfigure.mqtt.rpc;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 管理 MQTT 请求与响应的匹配、超时以及 topic 订阅。
 */
@Slf4j
public class MqttRequestResponseManager {

    private final SimpleMqttClient client;
    private final Map<String, ResponseTopicHandler> responseHandlers = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGenerator = new AtomicLong(0);
    private volatile long lastTimestamp = System.currentTimeMillis();

    public MqttRequestResponseManager(SimpleMqttClient client) {
        this.client = client;
    }

    public String send(String requestTopic,
                       String responseTopic,
                       Object requestMessage,
                       long timeout,
                       TimeUnit unit) {
        String requestId = generateRequestId();
        String requestPayload = buildRequestPayload(requestMessage, requestId);
        if (StrUtil.isBlank(requestPayload)) {
            throw new IllegalArgumentException("requestMessage must not be null");
        }

        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(requestPayload.getBytes());
        mqttMessage.setQos(1);

        MqttRequestContext context = new MqttRequestContext(
                requestTopic,
                responseTopic,
                mqttMessage,
                requestId,
                timeout,
                unit
        );
        return send(context);
    }

    private String send(MqttRequestContext context) {
        ResponseTopicHandler handler = responseHandlers.computeIfAbsent(
                context.getResponseTopic(),
                topic -> new ResponseTopicHandler(topic, client)
        );
        handler.ensureSubscribed();

        PendingRequest pendingRequest = handler.registerRequest(
                context.getRequestId(),
                context.getTimeoutMillis()
        );

        log.debug("📤 发送请求 - RequestId: {}, RequestTopic: {}, ResponseTopic: {}",
                context.getRequestId(), context.getRequestTopic(), context.getResponseTopic());

        handler.publishRequest(context.getRequestTopic(), context.getMqttMessage());

        try {
            String response = pendingRequest.getFuture().get(context.getTimeout(), context.getTimeUnit());
            log.debug("✅ 收到响应 - RequestId: {}, ResponseTopic: {}",
                    context.getRequestId(), context.getResponseTopic());
            return response;
        } catch (TimeoutException e) {
            throw new RuntimeException("Request timeout after " + context.getTimeout() + " " + context.getTimeUnit(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Request failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
    }

    private String generateRequestId() {
        long currentTime = System.currentTimeMillis();
        long sequence = requestIdGenerator.incrementAndGet();

        if (currentTime < lastTimestamp) {
            synchronized (this) {
                if (currentTime < lastTimestamp) {
                    requestIdGenerator.set(0);
                    sequence = requestIdGenerator.incrementAndGet();
                }
            }
        }
        lastTimestamp = currentTime;
        return currentTime + "-" + sequence;
    }

    private String buildRequestPayload(Object requestMessage, String requestId) {
        if (requestMessage instanceof String) {
            String payload = (String) requestMessage;
            if (payload.trim().startsWith("{")) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = (Map<String, Object>) JSON.parseObject(payload, Map.class);
                    json.put("requestId", requestId);
                    alignV2ThingMessageIdWithRequestId(json, requestId);
                    return JSON.toJSONString(json);
                } catch (Exception e) {
                    return payload;
                }
            }
            return payload;
        } else {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = (Map<String, Object>) JSON.parseObject(JSON.toJSONString(requestMessage), Map.class);
                json.put("requestId", requestId);
                alignV2ThingMessageIdWithRequestId(json, requestId);
                return JSON.toJSONString(json);
            } catch (Exception e) {
                return JSON.toJSONString(requestMessage);
            }
        }
    }

    /**
     * V2 物模型（阿里云 thing/*）请求体含 {@code version} + {@code params}，平台回复通常只回显 {@code id}。
     * 若 {@code id} 与 RPC 侧 {@link ResponseTopicHandler} 注册的 requestId 不一致，会误走 FIFO fallback，并发时可能错配响应。
     * 将 {@code id} 与内部 {@code requestId} 对齐，保证 reply 可被正确关联。
     */
    private static void alignV2ThingMessageIdWithRequestId(Map<String, Object> json, String requestId) {
        if (json == null || requestId == null) {
            return;
        }
        if (json.containsKey("params") && json.get("version") != null) {
            json.put("id", requestId);
        }
    }

    public void shutdown() {
        for (ResponseTopicHandler handler : responseHandlers.values()) {
            handler.shutdown();
        }
        responseHandlers.clear();
    }
}

