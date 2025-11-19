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
     * 设置响应处理器（模拟服务端）
     */
    private void setupResponseHandler(String responseTopic, String requestTopic, 
                                     java.util.function.Function<String, String> handler) {
        responseClient.subscribe(requestTopic, publish -> {
            try {
                String requestPayload = new String(publish.getPayloadAsBytes());
                log.debug("📨 收到请求: {}", requestPayload);
                
                // 处理请求
                String responsePayload = handler.apply(requestPayload);
                
                // 发送响应
                jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage responseMessage = 
                        new jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage();
                responseMessage.setPayload(responsePayload.getBytes());
                responseMessage.setQos(1);
                
                responseClient.publish(responseTopic, responseMessage);
                log.debug("📤 发送响应: {}", responsePayload);
            } catch (Exception e) {
                log.error("处理请求失败", e);
            }
        });
    }
}

