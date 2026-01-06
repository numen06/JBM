package jbm.framework.boot.autoconfigure.mqtt.rpc;

import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 负责管理某个响应 Topic 的所有 PendingRequest。
 */
@Slf4j
class ResponseTopicHandler {

    private static final int MAX_EXPIRED_CACHE = 2048;

    private final String responseTopic;
    private final SimpleMqttClient client;
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Queue<String> pendingOrder = new ConcurrentLinkedQueue<>();
    private final Set<String> expiredRequestIds = ConcurrentHashMap.newKeySet();
    private final Queue<String> expiredOrder = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService timeoutScheduler;
    private volatile boolean subscribed = false;

    ResponseTopicHandler(String responseTopic, SimpleMqttClient client) {
        this.responseTopic = responseTopic;
        this.client = client;
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mqtt-response-timeout-" + responseTopic.hashCode());
            thread.setDaemon(true);
            return thread;
        });
    }

    void ensureSubscribed() {
        if (subscribed) {
            return;
        }
        synchronized (this) {
            if (subscribed) {
                return;
            }
            client.subscribe(responseTopic, publish -> {
                try {
                    handleResponse(new String(publish.getPayloadAsBytes()));
                } catch (Exception e) {
                    log.error("❌ 处理响应消息失败 - Topic: {}", responseTopic, e);
                }
            });
            subscribed = true;
        }
    }

    PendingRequest registerRequest(String requestId, long timeoutMs) {
        PendingRequest pendingRequest = new PendingRequest(requestId);
        pendingRequests.put(requestId, pendingRequest);
        pendingOrder.offer(requestId);

        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            PendingRequest removed = removePendingRequest(requestId);
            if (removed != null && !removed.isCompleted()) {
                log.warn("⏰ 请求超时 - RequestId: {}, Topic: {}", requestId, responseTopic);
                removed.completeExceptionally(new TimeoutException("Request timeout after " + timeoutMs + "ms"));
                markRequestExpired(requestId);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        pendingRequest.setTimeoutFuture(timeoutTask);
        return pendingRequest;
    }

    void publishRequest(String requestTopic, MqttMessage message) {
        client.publish(requestTopic, message);
    }

    private void handleResponse(String payload) {
        try {
            String requestId = extractRequestId(payload);
            if (requestId != null) {
                PendingRequest request = removePendingRequest(requestId);
                if (request != null) {
                    request.complete(payload);
                    cancelTimeout(request);
                    return;
                }
                if (isExpired(requestId)) {
                    if (log.isDebugEnabled()) {
                        log.debug("⚠️ 收到已超时请求的响应，丢弃 - Topic: {}, RequestId: {}", responseTopic, requestId);
                    }
                    return;
                }
            }

            PendingRequest fallbackRequest = pollNextPendingRequest();
            if (fallbackRequest != null) {
                fallbackRequest.complete(payload);
                cancelTimeout(fallbackRequest);
            } else {
                log.debug("⚠️ 收到响应但无待处理请求 - Topic: {}", responseTopic);
            }
        } catch (Exception e) {
            log.error("❌ 处理响应失败 - Topic: {}", responseTopic, e);
        }
    }

    private String extractRequestId(String payload) {
        try {
            if (payload.trim().startsWith("{")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = (Map<String, Object>) JSON.parseObject(payload, Map.class);
                Object requestId = json.get("requestId");
                if (requestId == null) {
                    requestId = json.get("request_id");
                }
                if (requestId == null) {
                    requestId = json.get("id");
                }
                if (requestId == null) {
                    requestId = json.get("correlationId");
                }
                if (requestId != null) {
                    return requestId.toString();
                }
            }
        } catch (Exception e) {
            // ignore parse errors
        }
        return null;
    }

    private PendingRequest removePendingRequest(String requestId) {
        if (requestId == null) {
            return null;
        }
        PendingRequest request = pendingRequests.remove(requestId);
        if (request != null) {
            pendingOrder.remove(requestId);
        }
        return request;
    }

    private PendingRequest pollNextPendingRequest() {
        while (true) {
            String nextId = pendingOrder.poll();
            if (nextId == null) {
                return null;
            }
            PendingRequest request = pendingRequests.remove(nextId);
            if (request != null) {
                return request;
            }
        }
    }

    private void cancelTimeout(PendingRequest request) {
        ScheduledFuture<?> timeoutFuture = request.getTimeoutFuture();
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private void markRequestExpired(String requestId) {
        expiredRequestIds.add(requestId);
        expiredOrder.offer(requestId);
        trimExpiredCache();
    }

    private boolean isExpired(String requestId) {
        return expiredRequestIds.contains(requestId);
    }

    private void trimExpiredCache() {
        while (expiredOrder.size() > MAX_EXPIRED_CACHE) {
            String expiredId = expiredOrder.poll();
            if (expiredId != null) {
                expiredRequestIds.remove(expiredId);
            }
        }
    }

    void shutdown() {
        timeoutScheduler.shutdown();
        for (PendingRequest request : pendingRequests.values()) {
            pendingOrder.remove(request.getRequestId());
            cancelTimeout(request);
            if (!request.isCompleted()) {
                request.completeExceptionally(new CancellationException("Handler shutdown"));
            }
        }
        pendingRequests.clear();
        pendingOrder.clear();
        expiredRequestIds.clear();
        expiredOrder.clear();
    }
}

