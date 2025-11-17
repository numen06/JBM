package com.jbm.test.mqtt;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.jbm.test.mqtt.proxy.MqttSender;
import com.jbm.test.mqtt.proxy.MqttSender2;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
import jbm.framework.boot.autoconfigure.mqtt.registrar.EnableMqttMapperScan;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@EnableMqttMapperScan("com.jbm.test.mqtt")
@SpringBootTest(
        classes = {MqttAutoConfiguration.class, MqttExecuteImpl.class})
@Slf4j
public class MqttProxyTest {
    @Autowired
    private MqttProxyFactory mqttProxyFactory;

    @Autowired
    private MqttSender mqttSender;
    
    @Autowired
    private MqttSender2 mqttSender2;
    
    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;
    
    // 消息计数器，用于验证是否有重复订阅导致的重复消息
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    
    @BeforeEach
    public void setUp() throws Exception {
        messageCounter.set(0);
        log.info("========== 测试初始化完成 ==========");
    }

    /**
     * 基础功能测试
     */
    @Test
    public void test() {
        log.info("========== 基础功能测试 ==========");
        mqttSender.testfrom(MapUtil.of("test", "1212"));
        mqttSender2.toTwo("test2");
        ThreadUtil.sleep(2000);
        log.info("基础功能测试完成");
    }

    /**
     * 测试统计信息
     */
    @Test
    public void testStatistics() {
        log.info("========== 测试订阅统计信息 ==========");
        Map<String, Object> stats = mqttProxyFactory.getStatistics();
        
        assertNotNull(stats, "统计信息不应为空");
        assertTrue(stats.containsKey("totalClients"), "应包含总客户端数");
        assertTrue(stats.containsKey("totalSubscriptions"), "应包含总订阅数");
        assertTrue(stats.containsKey("connectedClients"), "应包含已连接客户端数");
        
        log.info("📊 统计信息: {}", stats);
        log.info("✅ 总客户端数: {}", stats.get("totalClients"));
        log.info("✅ 总订阅数: {}", stats.get("totalSubscriptions"));
        log.info("✅ 已连接客户端数: {}", stats.get("connectedClients"));
    }

    /**
     * 测试重复订阅检测
     * 验证同一主题不会被重复订阅
     */
    @Test
    public void testDuplicateSubscriptionDetection() throws Exception {
        log.info("========== 测试重复订阅检测 ==========");
        
        String testTopic = "/test/duplicate/check";
        SimpleMqttClient testClient = mqttPahoClientFactory.getAppClientInstance("duplicate-test-client");
        
        // 订阅主题并计数
        testClient.subscribe(testTopic, publish -> {
            int count = messageCounter.incrementAndGet();
            log.info("📨 接收到消息 #{}: {}", count, new String(publish.getPayloadAsBytes()));
        });
        
        // 等待订阅生效
        ThreadUtil.sleep(500);
        
        // 发送一条消息
        testClient.publishObject(testTopic, "测试重复订阅");
        
        // 等待消息处理
        ThreadUtil.sleep(1000);
        
        // 验证消息只被处理了一次
        int finalCount = messageCounter.get();
        log.info("✅ 消息处理次数: {}", finalCount);
        assertEquals(1, finalCount, "消息应该只被处理一次，而不是被重复处理");
    }

    /**
     * 并发订阅测试
     * 模拟多个线程同时注册订阅
     */
    @Test
    public void testConcurrentSubscription() throws Exception {
        log.info("========== 并发订阅测试 ==========");
        
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        List<Exception> exceptions = new CopyOnWriteArrayList<>();
        
        // 创建多个并发任务
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待所有线程就绪
                    
                    String topic = "/test/concurrent/topic";
                    SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("concurrent-test-client");
                    
                    client.subscribe(topic, publish -> {
                        log.info("🧵 线程 {} 接收到消息: {}", index, new String(publish.getPayloadAsBytes()));
                    });
                    
                    log.info("✅ 线程 {} 订阅完成", index);
                } catch (Exception e) {
                    log.error("❌ 线程 {} 订阅失败", index, e);
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        log.info("🚀 启动 {} 个并发线程...", threadCount);
        startLatch.countDown(); // 启动所有线程
        
        // 等待所有线程完成，最多等待10秒
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(finished, "所有线程应该在超时前完成");
        assertTrue(exceptions.isEmpty(), "不应该有异常: " + exceptions);
        
        log.info("✅ 并发订阅测试完成，无异常");
    }

    /**
     * 测试异常场景：空主题
     */
    @Test
    public void testSubscribeWithNullTopic() {
        log.info("========== 测试空主题异常处理 ==========");
        
        SimpleMqttClient testClient = mqttPahoClientFactory.getAppClientInstance("exception-test-client");
        
        assertThrows(Exception.class, () -> {
            testClient.subscribe(null, publish -> {
                log.info("不应该收到消息");
            });
        }, "订阅空主题应该抛出异常");
        
        log.info("✅ 空主题异常处理正确");
    }

    /**
     * 测试异常场景：空回调
     */
    @Test
    public void testSubscribeWithNullCallback() {
        log.info("========== 测试空回调异常处理 ==========");
        
        SimpleMqttClient testClient = mqttPahoClientFactory.getAppClientInstance("exception-test-client-2");
        
        assertThrows(Exception.class, () -> {
            testClient.subscribe("/test/null/callback", null);
        }, "订阅时传入空回调应该抛出异常");
        
        log.info("✅ 空回调异常处理正确");
    }

    /**
     * 大量消息并发处理测试
     */
    @Test
    public void testHighVolumeMessageProcessing() throws Exception {
        log.info("========== 大量消息并发处理测试 ==========");
        
        String testTopic = "/test/high/volume";
        SimpleMqttClient testClient = mqttPahoClientFactory.getAppClientInstance("high-volume-test-client");
        
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch messageLatch = new CountDownLatch(100);
        
        // 订阅主题
        testClient.subscribe(testTopic, publish -> {
            receivedCount.incrementAndGet();
            messageLatch.countDown();
            log.debug("📨 接收消息: {}", new String(publish.getPayloadAsBytes()));
        });
        
        // 等待订阅生效
        ThreadUtil.sleep(500);
        
        // 并发发送大量消息
        ExecutorService publishExecutor = Executors.newFixedThreadPool(5);
        int messageCount = 100;
        
        log.info("📤 开始发送 {} 条消息...", messageCount);
        for (int i = 0; i < messageCount; i++) {
            final int msgIndex = i;
            publishExecutor.submit(() -> {
                try {
                    testClient.publishObject(testTopic, "消息-" + msgIndex);
                } catch (Exception e) {
                    log.error("发送消息失败: {}", msgIndex, e);
                }
            });
        }
        
        publishExecutor.shutdown();
        publishExecutor.awaitTermination(10, TimeUnit.SECONDS);
        
        // 等待所有消息被接收
        boolean allReceived = messageLatch.await(15, TimeUnit.SECONDS);
        
        assertTrue(allReceived, "应该接收到所有消息");
        assertEquals(messageCount, receivedCount.get(), "接收消息数应该等于发送消息数");
        
        log.info("✅ 大量消息测试完成，发送: {}, 接收: {}", messageCount, receivedCount.get());
    }

    /**
     * 测试客户端重连后订阅恢复
     */
    @Test
    public void testSubscriptionRestoreAfterReconnect() throws Exception {
        log.info("========== 测试订阅恢复功能 ==========");
        
        try {
            // 恢复所有订阅
            mqttProxyFactory.restoreAllSubscriptions();
            log.info("✅ 订阅恢复完成");
            
            // 验证客户端状态
            Map<String, Object> stats = mqttProxyFactory.getStatistics();
            log.info("📊 恢复后的统计信息: {}", stats);
            
            assertNotNull(stats.get("totalSubscriptions"), "应该有订阅记录");
        } catch (Exception e) {
            log.error("❌ 订阅恢复测试失败", e);
            throw e;
        }
    }

    /**
     * 测试多个 Mapper 订阅相同主题
     */
    @Test
    public void testMultipleMappersSameTopic() throws Exception {
        log.info("========== 测试多个 Mapper 订阅相同主题 ==========");
        
        String sharedTopic = "/test/shared/topic";
        AtomicInteger handler1Count = new AtomicInteger(0);
        AtomicInteger handler2Count = new AtomicInteger(0);
        
        SimpleMqttClient client1 = mqttPahoClientFactory.getAppClientInstance("mapper1-client");
        SimpleMqttClient client2 = mqttPahoClientFactory.getAppClientInstance("mapper2-client");
        
        // 两个处理器订阅相同主题
        client1.subscribe(sharedTopic, publish -> {
            handler1Count.incrementAndGet();
            log.info("📨 Handler1 接收: {}", new String(publish.getPayloadAsBytes()));
        });
        
        client2.subscribe(sharedTopic, publish -> {
            handler2Count.incrementAndGet();
            log.info("📨 Handler2 接收: {}", new String(publish.getPayloadAsBytes()));
        });
        
        ThreadUtil.sleep(500);
        
        // 发送消息
        client1.publishObject(sharedTopic, "共享主题测试消息");
        
        ThreadUtil.sleep(1000);
        
        log.info("✅ Handler1 接收次数: {}", handler1Count.get());
        log.info("✅ Handler2 接收次数: {}", handler2Count.get());
        
        // 验证两个处理器都收到了消息
        assertTrue(handler1Count.get() > 0, "Handler1 应该接收到消息");
        assertTrue(handler2Count.get() > 0, "Handler2 应该接收到消息");
    }

    /**
     * 压力测试：长时间运行
     */
    @Test
    public void testLongRunningStability() throws Exception {
        log.info("========== 长时间运行稳定性测试 ==========");
        
        String testTopic = "/test/stability";
        SimpleMqttClient testClient = mqttPahoClientFactory.getAppClientInstance("stability-test-client");
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        testClient.subscribe(testTopic, publish -> {
            successCount.incrementAndGet();
        });
        
        ThreadUtil.sleep(500);
        
        // 持续发送消息 30 秒
        long endTime = System.currentTimeMillis() + 30_000;
        int messagesSent = 0;
        
        log.info("🔄 开始 30 秒稳定性测试...");
        while (System.currentTimeMillis() < endTime) {
            try {
                testClient.publishObject(testTopic, "稳定性测试-" + messagesSent);
                messagesSent++;
                ThreadUtil.sleep(100); // 每100ms发送一条
            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("发送失败", e);
            }
        }
        
        ThreadUtil.sleep(2000); // 等待最后的消息处理完成
        
        log.info("✅ 发送消息总数: {}", messagesSent);
        log.info("✅ 成功接收数: {}", successCount.get());
        log.info("✅ 错误数: {}", errorCount.get());
        
        assertTrue(errorCount.get() < messagesSent * 0.01, "错误率应该低于1%");
        assertTrue(successCount.get() > messagesSent * 0.95, "成功率应该高于95%");
    }

    /**
     * 测试所有客户端获取
     */
    @Test
    public void testGetAllClients() {
        log.info("========== 测试获取所有客户端 ==========");

        Collection<SimpleMqttClient> clients = mqttProxyFactory.getAllClients();
        assertNotNull(clients, "客户端列表不应为空");
        
        log.info("✅ 总客户端数: {}", clients.size());
        clients.forEach(client -> {
            log.info("📱 客户端连接状态: {}", client.isConnected());
        });
    }
}
