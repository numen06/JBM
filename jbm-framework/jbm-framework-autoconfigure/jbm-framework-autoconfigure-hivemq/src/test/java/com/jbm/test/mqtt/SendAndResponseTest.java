package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.factories.Mqtt5ClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 sendAndResponse 方法的优化功能
 * 包括：基本请求-响应、并发请求、超时处理、复用订阅等
 */
@Slf4j
public class SendAndResponseTest {

    private static RealMqttPahoClientFactory mqttPahoClientFactory;
    private static SimpleMqttClient requestClient;
    private static SimpleMqttClient responseClient;
    private static final ConcurrentMap<String, RequestTopicDispatcher> requestDispatchers = new ConcurrentHashMap<>();

    @BeforeAll
    public static void setup() throws Exception {
        Mqtt5ClientFactory mqtt5ClientFactory = new Mqtt5ClientFactory();
        MqttProperties mqttProperties = new MqttProperties();
        mqttProperties.setUrl(URI.create("tcp://10.100.10.121:1883"));
        mqttPahoClientFactory = new RealMqttPahoClientFactory(mqtt5ClientFactory, mqttProperties);
        
        // 创建请求客户端和响应客户端
        requestClient = mqttPahoClientFactory.getAppClientInstance("send-response-test-request");
        responseClient = mqttPahoClientFactory.getAppClientInstance("send-response-test-response");
        
        // 等待连接建立
        ThreadUtil.sleep(1000);
    }

    /**
     * 测试基本的请求-响应功能
     */
    @Test
    public void testBasicRequestResponse() throws Exception {
        log.info("========== 测试：基本请求-响应 ==========");
        
        String requestTopic = "/test/request/basic";
        String responseTopic = "/test/response/basic";
        
        // 设置响应处理器（模拟服务端）
        setupResponseHandler(responseTopic, requestTopic, (requestPayload) -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> request = (Map<String, Object>) JSON.parseObject(requestPayload, Map.class);
                String requestId = (String) request.get("requestId");
                String message = (String) request.getOrDefault("message", "Hello");
                
                // 构建响应
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("requestId", requestId);
                response.put("result", "Echo: " + message);
                response.put("status", "success");
                
                return JSON.toJSONString(response);
            } catch (Exception e) {
                log.error("处理请求失败", e);
                return "{\"error\":\"处理失败\"}";
            }
        });
        
        ThreadUtil.sleep(500);
        
        // 发送请求
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("message", "Test Message");
        
        String response = requestClient.sendAndResponse(requestTopic, responseTopic, request, 10, TimeUnit.SECONDS);
        
        assertNotNull(response, "响应不应为空");
        log.info("✅ 收到响应: {}", response);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) JSON.parseObject(response, Map.class);
        assertEquals("success", responseMap.get("status"), "响应状态应为success");
        assertTrue(responseMap.containsKey("result"), "响应应包含result字段");
    }

    /**
     * 测试并发请求（同一响应topic）
     */
    @Test
    public void testConcurrentRequests() throws Exception {
        log.info("========== 测试：并发请求（复用订阅） ==========");
        
        String requestTopic = "/test/request/concurrent";
        String responseTopic = "/test/response/concurrent";
        
        AtomicInteger requestCount = new AtomicInteger(0);
        
        // 设置响应处理器
        setupResponseHandler(responseTopic, requestTopic, (requestPayload) -> {
            int count = requestCount.incrementAndGet();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> request = (Map<String, Object>) JSON.parseObject(requestPayload, Map.class);
                String requestId = (String) request.get("requestId");
                int requestNum = (Integer) request.getOrDefault("num", 0);
                
                // 模拟处理延迟
                ThreadUtil.sleep(100);
                
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("requestId", requestId);
                response.put("result", "Processed request #" + requestNum);
                response.put("count", count);
                
                return JSON.toJSONString(response);
            } catch (Exception e) {
                log.error("处理请求失败", e);
                return "{\"error\":\"处理失败\"}";
            }
        });
        
        ThreadUtil.sleep(500);
        
        int concurrentCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentCount);
        CountDownLatch latch = new CountDownLatch(concurrentCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 并发发送请求
        log.info("📤 并发发送 {} 个请求", concurrentCount);
        for (int i = 0; i < concurrentCount; i++) {
            final int requestNum = i;
            executor.submit(() -> {
                try {
                    Map<String, Object> request = new java.util.HashMap<>();
                    request.put("num", requestNum);
                    request.put("message", "Concurrent request #" + requestNum);
                    
                    String response = requestClient.sendAndResponse(
                            requestTopic, 
                            responseTopic, 
                            request, 
                            5, 
                            TimeUnit.SECONDS
                    );
                    
                    assertNotNull(response, "响应不应为空");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) JSON.parseObject(response, Map.class);
                    assertTrue(responseMap.containsKey("result"), "响应应包含result");
                    
                    successCount.incrementAndGet();
                    log.info("✅ 请求 #{} 完成", requestNum);
                } catch (Exception e) {
                    log.error("❌ 请求 #{} 失败", requestNum, e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有请求完成
        boolean allCompleted = latch.await(30, TimeUnit.SECONDS);
        assertTrue(allCompleted, "所有请求应在超时前完成");
        
        executor.shutdown();
        
        log.info("✅ 并发测试完成 - 成功: {}/{}", successCount.get(), concurrentCount);
        assertEquals(concurrentCount, successCount.get(), "所有并发请求应该成功");
        assertEquals(concurrentCount, requestCount.get(), "服务端应该处理所有请求");
    }

    /**
     * 测试超时处理
     */
    @Test
    public void testTimeout() throws Exception {
        log.info("========== 测试：请求超时 ==========");
        
        String requestTopic = "/test/request/timeout";
        String responseTopic = "/test/response/timeout";
        
        // 设置响应处理器（延迟响应，导致超时）
        setupResponseHandler(responseTopic, requestTopic, (requestPayload) -> {
            // 延迟5秒响应（超过3秒超时）
            ThreadUtil.sleep(5000);
            return "{\"result\":\"too late\"}";
        });
        
        ThreadUtil.sleep(500);
        
        // 发送请求（3秒超时）
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("message", "Timeout test");
        
        long startTime = System.currentTimeMillis();
        try {
            requestClient.sendAndResponse(requestTopic, responseTopic, request, 3, TimeUnit.SECONDS);
            fail("应该抛出超时异常");
        } catch (RuntimeException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            assertTrue(e.getMessage().contains("timeout") || e.getMessage().contains("timeout"), 
                    "异常信息应包含timeout");
            assertTrue(elapsed >= 2900 && elapsed < 4000, 
                    "应该在3秒左右超时，实际: " + elapsed + "ms");
            log.info("✅ 超时测试通过 - 耗时: {}ms", elapsed);
        }
    }

    /**
     * 测试复用订阅（多次调用同一响应topic）
     */
    @Test
    public void testSubscriptionReuse() throws Exception {
        log.info("========== 测试：复用订阅 ==========");
        
        String requestTopic = "/test/request/reuse";
        String responseTopic = "/test/response/reuse";
        
        AtomicInteger handlerCallCount = new AtomicInteger(0);
        
        // 设置响应处理器
        setupResponseHandler(responseTopic, requestTopic, (requestPayload) -> {
            handlerCallCount.incrementAndGet();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> request = (Map<String, Object>) JSON.parseObject(requestPayload, Map.class);
                String requestId = (String) request.get("requestId");
                
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("requestId", requestId);
                response.put("result", "OK");
                
                return JSON.toJSONString(response);
            } catch (Exception e) {
                return "{\"error\":\"处理失败\"}";
            }
        });
        
        ThreadUtil.sleep(500);
        
        // 连续发送多个请求（应该复用同一个订阅）
        int requestCount = 5;
        log.info("📤 连续发送 {} 个请求（测试订阅复用）", requestCount);
        
        for (int i = 0; i < requestCount; i++) {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("message", "Request #" + i);
            
            String response = requestClient.sendAndResponse(
                    requestTopic, 
                    responseTopic, 
                    request, 
                    5, 
                    TimeUnit.SECONDS
            );
            
            assertNotNull(response, "响应不应为空");
            ThreadUtil.sleep(200);
        }
        
        log.info("✅ 订阅复用测试完成 - 处理器调用次数: {}", handlerCallCount.get());
        assertEquals(requestCount, handlerCallCount.get(), "所有请求应该被处理");
    }

    /**
     * 测试请求ID匹配
     */
    @Test
    public void testRequestIdMatching() throws Exception {
        log.info("========== 测试：请求ID匹配 ==========");
        
        String requestTopic = "/test/request/idmatch";
        String responseTopic = "/test/response/idmatch";
        
        // 设置响应处理器（返回请求ID）
        setupResponseHandler(responseTopic, requestTopic, (requestPayload) -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> request = (Map<String, Object>) JSON.parseObject(requestPayload, Map.class);
                String requestId = (String) request.get("requestId");
                
                // 响应中包含请求ID
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("requestId", requestId);
                response.put("result", "Matched by ID");
                
                return JSON.toJSONString(response);
            } catch (Exception e) {
                return "{\"error\":\"处理失败\"}";
            }
        });
        
        ThreadUtil.sleep(500);
        
        // 并发发送多个请求，测试ID匹配
        int requestCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicInteger matchedCount = new AtomicInteger(0);
        
        for (int i = 0; i < requestCount; i++) {
            final int requestNum = i;
            executor.submit(() -> {
                try {
                    Map<String, Object> request = new java.util.HashMap<>();
                    request.put("num", requestNum);
                    
                    String response = requestClient.sendAndResponse(
                            requestTopic, 
                            responseTopic, 
                            request, 
                            5, 
                            TimeUnit.SECONDS
                    );
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) JSON.parseObject(response, Map.class);
                    if (responseMap.containsKey("requestId") && 
                        responseMap.get("result").equals("Matched by ID")) {
                        matchedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("请求失败", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        log.info("✅ 请求ID匹配测试完成 - 匹配成功: {}/{}", matchedCount.get(), requestCount);
        assertEquals(requestCount, matchedCount.get(), "所有请求应该通过ID正确匹配");
    }

    /**
     * 测试频繁请求（模拟实际场景）
     */
    @Test
    public void testFrequentRequests() throws Exception {
        log.info("========== 测试：频繁请求 ==========");
        
        String requestTopic = "/test/request/frequent";
        String responseTopic = "/test/response/frequent";
        
        AtomicInteger processedCount = new AtomicInteger(0);
        
        // 设置响应处理器
        setupResponseHandler(responseTopic, requestTopic, (requestPayload) -> {
            processedCount.incrementAndGet();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> request = (Map<String, Object>) JSON.parseObject(requestPayload, Map.class);
                String requestId = (String) request.get("requestId");
                
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("requestId", requestId);
                response.put("result", "Processed");
                
                return JSON.toJSONString(response);
            } catch (Exception e) {
                return "{\"error\":\"处理失败\"}";
            }
        });
        
        ThreadUtil.sleep(500);
        
        // 快速连续发送请求
        int requestCount = 20;
        log.info("📤 快速发送 {} 个请求", requestCount);
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("index", i);
            
            String response = requestClient.sendAndResponse(
                    requestTopic, 
                    responseTopic, 
                    request, 
                    5, 
                    TimeUnit.SECONDS
            );
            
            assertNotNull(response, "响应不应为空");
        }
        long elapsed = System.currentTimeMillis() - startTime;
        
        log.info("✅ 频繁请求测试完成 - 处理: {}/{}, 耗时: {}ms", 
                processedCount.get(), requestCount, elapsed);
        assertEquals(requestCount, processedCount.get(), "所有请求应该被处理");
    }

    /**
     * 测试不同地址交叉并发请求（模拟真实场景）
     */
    @Test
    public void testCrossAddressConcurrentRequests() throws Exception {
        log.info("========== 测试：不同地址交叉并发请求 ==========");
        
        // 定义多个不同的地址组合
        String[][] addressPairs = {
            {"/test/request/service1", "/test/response/service1"},
            {"/test/request/service2", "/test/response/service2"},
            {"/test/request/service3", "/test/response/service3"},
            {"/test/request/service4", "/test/response/service4"}, // 使用独立的 requestTopic，避免覆盖订阅
            {"/test/request/service5", "/test/response/service1"}, // 不同的 requestTopic，相同的 responseTopic
        };
        
        AtomicInteger[] handlerCounts = new AtomicInteger[addressPairs.length];
        for (int i = 0; i < handlerCounts.length; i++) {
            handlerCounts[i] = new AtomicInteger(0);
        }
        
        // 为每个地址组合设置响应处理器
        for (int i = 0; i < addressPairs.length; i++) {
            final int index = i;
            String requestTopic = addressPairs[i][0];
            String responseTopic = addressPairs[i][1];
            
            setupResponseHandler(index, responseTopic, requestTopic, (requestPayload) -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> request = (Map<String, Object>) JSON.parseObject(requestPayload, Map.class);
                    handlerCounts[index].incrementAndGet();
                    String requestId = (String) request.get("requestId");
                    String serviceName = "Service" + (index + 1);
                    
                    // 模拟处理延迟
                    ThreadUtil.sleep(50);
                    
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("requestId", requestId);
                    response.put("service", serviceName);
                    response.put("result", "Processed by " + serviceName);
                    
                    return JSON.toJSONString(response);
                } catch (Exception e) {
                    log.error("处理请求失败", e);
                    return "{\"error\":\"处理失败\"}";
                }
            });
        }
        
        ThreadUtil.sleep(1000); // 等待所有订阅完成
        
        // 并发发送请求到不同的地址
        int requestsPerAddress = 5;
        int totalRequests = addressPairs.length * requestsPerAddress;
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        log.info("📤 并发发送 {} 个请求到 {} 个不同的地址组合", totalRequests, addressPairs.length);
        
        for (int i = 0; i < addressPairs.length; i++) {
            final int addressIndex = i;
            String requestTopic = addressPairs[i][0];
            String responseTopic = addressPairs[i][1];
            
            for (int j = 0; j < requestsPerAddress; j++) {
                final int requestNum = j;
                executor.submit(() -> {
                    try {
                        Map<String, Object> request = new java.util.HashMap<>();
                        request.put("addressIndex", addressIndex);
                        request.put("requestNum", requestNum);
                        request.put("message", "Cross address request #" + requestNum);
                        
                        String response = requestClient.sendAndResponse(
                                requestTopic,
                                responseTopic,
                                request,
                                10,
                                TimeUnit.SECONDS
                        );
                        
                        assertNotNull(response, "响应不应为空");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = (Map<String, Object>) JSON.parseObject(response, Map.class);
                        
                        // 验证响应来自正确的服务
                        String service = (String) responseMap.get("service");
                        assertNotNull(service, "响应应包含service字段");
                        assertTrue(service.contains("Service" + (addressIndex + 1)), 
                                "响应应来自正确的服务");
                        
                        successCount.incrementAndGet();
                        log.debug("✅ 地址[{}] 请求[{}] 完成 - Service: {}", 
                                addressIndex, requestNum, service);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("❌ 地址[{}] 请求[{}] 失败", addressIndex, requestNum, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        // 等待所有请求完成
        boolean allCompleted = latch.await(60, TimeUnit.SECONDS);
        assertTrue(allCompleted, "所有请求应在超时前完成");
        
        executor.shutdown();
        
        log.info("✅ 交叉并发测试完成");
        log.info("   - 总请求数: {}", totalRequests);
        log.info("   - 成功: {}", successCount.get());
        log.info("   - 失败: {}", errorCount.get());
        
        // 验证每个地址的请求都被正确处理
        for (int i = 0; i < addressPairs.length; i++) {
            int processed = handlerCounts[i].get();
            log.info("   - 地址[{}] ({}) 处理: {}/{}", 
                    i, addressPairs[i][1], processed, requestsPerAddress);
            assertEquals(requestsPerAddress, processed, 
                    "地址[" + i + "] 应该处理所有请求");
        }
        
        assertEquals(totalRequests, successCount.get(), 
                "所有请求应该成功");
        assertEquals(0, errorCount.get(), 
                "不应该有失败的请求");
    }

    /**
     * 测试：响应中不包含 requestId 时也能保持顺序匹配
     */
    @Test
    public void testResponsesWithoutRequestId() throws Exception {
        log.info("========== 测试：响应不包含 requestId ==========");
        
        String requestTopic = "/test/request/noRequestId";
        String responseTopic = "/test/response/noRequestId";
        
        AtomicInteger processed = new AtomicInteger(0);
        
        // 设置一个不返回 requestId 的响应处理器（使用 subscribeAndWait 确保订阅完成）
        int retries = 0;
        while (!responseClient.isConnected() && retries < 50) {
            ThreadUtil.sleep(100);
            retries++;
        }
        boolean ok = responseClient.subscribeAndWait(requestTopic, publish -> {
            try {
                String payload = new String(publish.getPayloadAsBytes());
                @SuppressWarnings("unchecked")
                Map<String, Object> request = (Map<String, Object>) JSON.parseObject(payload, Map.class);
                
                ThreadUtil.sleep(50);
                
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("result", "OK");
                response.put("echo", request.get("message"));
                response.put("seq", processed.incrementAndGet());
                
                jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage responseMessage =
                        new jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage();
                responseMessage.setPayload(JSON.toJSONString(response).getBytes());
                responseMessage.setQos(1);
                responseClient.publish(responseTopic, responseMessage);
            } catch (Exception e) {
                log.error("处理请求失败", e);
            }
        }, 10, TimeUnit.SECONDS);
        assertTrue(ok, "订阅应在10秒内完成");
        
        // 无 requestId 时依赖 FIFO 匹配，需顺序执行以保证请求与响应一一对应
        int requestCount = 10;
        for (int i = 0; i < requestCount; i++) {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("message", "no-id-" + i);
            
            String response = requestClient.sendAndResponse(
                    requestTopic,
                    responseTopic,
                    request,
                    10,
                    TimeUnit.SECONDS
            );
            
            assertNotNull(response, "响应不应为空");
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>) JSON.parseObject(response, Map.class);
            assertEquals("OK", responseMap.get("result"));
            assertEquals("no-id-" + i, responseMap.get("echo"),
                    "响应应该与对应的请求匹配");
        }
    }

    /**
     * 设置响应处理器（模拟服务端）
     */
    private void setupResponseHandler(String responseTopic, String requestTopic, 
                                     java.util.function.Function<String, String> handler) {
        setupResponseHandler(-1, responseTopic, requestTopic, handler);
    }
    
    private void setupResponseHandler(int addressIndex, String responseTopic, String requestTopic,
                                     java.util.function.Function<String, String> handler) {
        RequestTopicDispatcher dispatcher = requestDispatchers.computeIfAbsent(requestTopic,
                topic -> new RequestTopicDispatcher(responseClient, topic));
        dispatcher.register(addressIndex, responseTopic, handler);
    }
    
    private static class RequestTopicDispatcher {
        private final SimpleMqttClient client;
        private final String requestTopic;
        private final CopyOnWriteArrayList<HandlerRegistration> handlers = new CopyOnWriteArrayList<>();
        private volatile boolean subscribed = false;
        
        public RequestTopicDispatcher(SimpleMqttClient client, String requestTopic) {
            this.client = client;
            this.requestTopic = requestTopic;
        }
        
        public void register(int addressIndex, String responseTopic,
                             java.util.function.Function<String, String> handler) {
            handlers.add(new HandlerRegistration(addressIndex, responseTopic, handler));
            ensureSubscribed();
        }
        
        private void ensureSubscribed() {
            if (subscribed) {
                return;
            }
            synchronized (this) {
                if (subscribed) {
                    return;
                }
                int retries = 0;
                while (!client.isConnected() && retries < 50) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    retries++;
                }
                boolean ok = client.subscribeAndWait(requestTopic, publish -> {
                    String payload = new String(publish.getPayloadAsBytes());
                    handleMessage(payload);
                }, 10, TimeUnit.SECONDS);
                if (!ok) {
                    throw new IllegalStateException("Subscribe failed for topic: " + requestTopic);
                }
                subscribed = true;
            }
        }
        
        private void handleMessage(String payload) {
            Map<String, Object> cachedRequest = null;
            for (HandlerRegistration registration : handlers) {
                try {
                    if (registration.addressIndex >= 0) {
                        if (cachedRequest == null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = (Map<String, Object>) JSON.parseObject(payload, Map.class);
                            cachedRequest = parsed;
                        }
                        Object idx = cachedRequest.get("addressIndex");
                        if (!(idx instanceof Number) ||
                                ((Number) idx).intValue() != registration.addressIndex) {
                            continue;
                        }
                    }
                    String responsePayload = registration.handler.apply(payload);
                    if (responsePayload == null) {
                        continue;
                    }
                    jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage responseMessage =
                            new jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage();
                    responseMessage.setPayload(responsePayload.getBytes());
                    responseMessage.setQos(1);
                    client.publish(registration.responseTopic, responseMessage);
                    log.debug("📤 发送响应: {}", responsePayload);
                } catch (Exception e) {
                    log.error("处理请求失败 - Topic: {}", requestTopic, e);
                }
            }
        }
    }
    
    private static class HandlerRegistration {
        private final int addressIndex;
        private final String responseTopic;
        private final java.util.function.Function<String, String> handler;
        
        public HandlerRegistration(int addressIndex, String responseTopic,
                                   java.util.function.Function<String, String> handler) {
            this.addressIndex = addressIndex;
            this.responseTopic = responseTopic;
            this.handler = handler;
        }
    }
}

