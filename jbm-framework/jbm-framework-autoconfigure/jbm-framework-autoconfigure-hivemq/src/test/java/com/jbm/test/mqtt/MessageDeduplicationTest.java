package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.registrar.EnableMqttMapperScan;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息处理测试
 * 测试修复后的系统：发送N条消息，就执行N次方法
 * 核心修复：通过监听器缓存防止重复创建 MqttRequestListener，避免同一条消息被多次处理
 */
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@EnableMqttMapperScan("com.jbm.test.mqtt")
@SpringBootTest(classes = {MqttAutoConfiguration.class, MqttExecuteImpl.class})
@Slf4j
public class MessageDeduplicationTest {

    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;

    /**
     * 测试场景1: 验证同一消息不会被重复执行
     * 问题描述：订阅 topic 只收到了一次，但代码执行了两次
     */
    @Test
    public void testMessageExecutedOnlyOnce() throws Exception {
        log.info("========== 测试场景1: 验证消息只执行一次 ==========");
        
        String testTopic = "/test/dedup/once";
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("dedup-test-client-1");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 第 {} 次执行处理逻辑", count);
            latch.countDown();
        });
        
        ThreadUtil.sleep(500);
        
        // 发送一条消息
        log.info("📤 发送测试消息");
        client.publishObject(testTopic, "测试消息-场景1");
        
        // 等待消息处理
        boolean processed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(processed, "消息应该被处理");
        
        // 再等待一段时间，看是否有重复执行
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 最终执行次数: {}", finalCount);
        
        assertEquals(1, finalCount, "消息应该只被执行一次，而不是执行多次");
    }

    /**
     * 测试场景2: 快速连续发送相同内容的消息
     */
    @Test
    public void testRapidSameMessages() throws Exception {
        log.info("========== 测试场景2: 快速连续发送相同消息 ==========");
        
        String testTopic = "/test/messages/rapid";
        AtomicInteger executionCount = new AtomicInteger(0);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("rapid-test-client");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 执行处理逻辑 #{}: {}", count, new String(publish.getPayloadAsBytes()));
        });
        
        ThreadUtil.sleep(500);
        
        String message = "相同内容的消息";
        int sendCount = 3;
        
        // 快速发送3条相同内容的消息
        log.info("📤 快速发送{}条相同消息（间隔100ms）", sendCount);
        for (int i = 0; i < sendCount; i++) {
            client.publishObject(testTopic, message);
            ThreadUtil.sleep(100); // 短暂间隔
        }
        
        // 等待处理
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: {}, 执行: {}", sendCount, finalCount);
        
        // 发送3条，应该执行3次
        assertEquals(sendCount, finalCount, "发送" + sendCount + "条消息，应该执行" + sendCount + "次");
    }

    /**
     * 测试场景3: 多次发送相同内容的消息
     */
    @Test
    public void testMultipleSameMessages() throws Exception {
        log.info("========== 测试场景3: 多次发送相同消息 ==========");
        
        String testTopic = "/test/messages/multiple-same";
        AtomicInteger executionCount = new AtomicInteger(0);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("multiple-test-client");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 执行处理逻辑 #{}: {}", count, new String(publish.getPayloadAsBytes()));
        });
        
        ThreadUtil.sleep(500);
        
        String message = "相同内容的消息";
        int sendCount = 5;
        
        // 分多次发送相同消息
        log.info("📤 发送{}条相同消息", sendCount);
        for (int i = 0; i < sendCount; i++) {
            client.publishObject(testTopic, message);
            ThreadUtil.sleep(200); // 间隔200ms
            log.info("已发送 {}/{} 条", i + 1, sendCount);
        }
        
        // 等待处理
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: {}, 执行: {}", sendCount, finalCount);
        
        assertEquals(sendCount, finalCount, "发送" + sendCount + "条消息，应该执行" + sendCount + "次");
    }

    /**
     * 测试场景4: 不同内容的消息不应该被去重
     */
    @Test
    public void testDifferentContentMessages() throws Exception {
        log.info("========== 测试场景4: 不同内容的消息 ==========");
        
        String testTopic = "/test/dedup/different-content";
        AtomicInteger executionCount = new AtomicInteger(0);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("dedup-test-client-4");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 执行处理逻辑 #{}: {}", count, new String(publish.getPayloadAsBytes()));
        });
        
        ThreadUtil.sleep(500);
        
        // 发送多条不同内容的消息
        log.info("📤 发送5条不同内容的消息");
        for (int i = 0; i < 5; i++) {
            client.publishObject(testTopic, "消息-" + i);
            ThreadUtil.sleep(100);
        }
        
        // 等待处理
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 执行次数: {}", finalCount);
        
        assertEquals(5, finalCount, "不同内容的消息都应该被处理");
    }

    /**
     * 测试场景5: 并发场景下的消息处理
     */
    @Test
    public void testConcurrentMessages() throws Exception {
        log.info("========== 测试场景5: 并发场景下的消息处理 ==========");
        
        String testTopic = "/test/messages/concurrent";
        AtomicInteger executionCount = new AtomicInteger(0);
        int sendCount = 10;
        CountDownLatch publishLatch = new CountDownLatch(sendCount);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("concurrent-test-client");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 执行处理逻辑 #{}: {}", count, new String(publish.getPayloadAsBytes()));
        });
        
        ThreadUtil.sleep(500);
        
        // 并发发送10条不同的消息
        log.info("📤 并发发送{}条消息", sendCount);
        for (int i = 0; i < sendCount; i++) {
            final int msgIndex = i;
            new Thread(() -> {
                try {
                    client.publishObject(testTopic, "并发消息-" + msgIndex);
                } catch (Exception e) {
                    log.error("发送失败", e);
                } finally {
                    publishLatch.countDown();
                }
            }).start();
        }
        
        // 等待所有消息发送完成
        boolean allSent = publishLatch.await(5, TimeUnit.SECONDS);
        assertTrue(allSent, "所有消息应该在超时前发送完成");
        
        // 等待处理
        ThreadUtil.sleep(3000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: {}, 执行: {}", sendCount, finalCount);
        
        // 并发发送10条，应该执行10次
        assertEquals(sendCount, finalCount, "并发发送" + sendCount + "条消息，应该执行" + sendCount + "次");
    }

    /**
     * 测试场景6: 正常消息流处理
     */
    @Test
    public void testNormalMessageFlow() throws Exception {
        log.info("========== 测试场景6: 正常消息流处理 ==========");
        
        String testTopic = "/test/messages/normal";
        AtomicInteger executionCount = new AtomicInteger(0);
        int sendCount = 10;
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("normal-flow-client");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            String content = new String(publish.getPayloadAsBytes());
            log.info("📨 执行处理逻辑 #{}: {}", count, content);
        });
        
        ThreadUtil.sleep(500);
        
        // 发送一系列不同内容的消息
        log.info("📤 发送{}条不同内容的消息", sendCount);
        for (int i = 0; i < sendCount; i++) {
            client.publishObject(testTopic, "消息-" + i);
            ThreadUtil.sleep(100); // 短间隔
        }
        
        // 等待处理
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: {}, 执行: {}", sendCount, finalCount);
        
        assertEquals(sendCount, finalCount, "发送" + sendCount + "条消息，应该执行" + sendCount + "次");
    }

    /**
     * 测试场景7: 模拟实际问题 - 验证修复效果
     */
    @Test
    public void testRealWorldScenario() throws Exception {
        log.info("========== 测试场景7: 模拟实际问题场景（验证修复）==========");
        
        String testTopic = "/test/from";  // 使用实际的主题
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("real-world-test");
        
        // 订阅主题（模拟实际的 MqttMapper）
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            String content = new String(publish.getPayloadAsBytes());
            log.info("📨 第 {} 次执行业务逻辑: {}", count, content);
            
            // 模拟业务处理
            try {
                ThreadUtil.sleep(100);
                log.info("✅ 业务处理完成");
            } catch (Exception e) {
                log.error("业务处理失败", e);
            }
            
            if (count == 1) {
                latch.countDown();
            }
        });
        
        ThreadUtil.sleep(500);
        
        // 发送消息
        log.info("📤 发送1条业务消息");
        client.publishObject(testTopic, "{\"test\":\"data\"}");
        
        // 等待第一次处理
        boolean firstExecution = latch.await(3, TimeUnit.SECONDS);
        assertTrue(firstExecution, "消息应该被处理");
        
        log.info("⏱️ 第一次执行完成，等待2秒确认没有重复执行...");
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 最终执行次数: {}", finalCount);
        
        if (finalCount > 1) {
            log.error("❌ 修复失败！消息只收到1次，但执行了{}次", finalCount);
        } else {
            log.info("✅ 修复成功！消息只执行了1次，符合预期");
        }
        
        assertEquals(1, finalCount, 
                "问题修复验证：发送1条消息，应该只执行1次，而不是执行" + finalCount + "次");
    }

    /**
     * 测试场景8: 稳定性测试（简化版）
     * 注意：此测试类加载了 MqttExecuteImpl，不适合做长时间测试
     * 长时间稳定性测试请使用 SimpleMessageTest.testLongRunningStability()
     */
    @Test
    public void testStability() throws Exception {
        log.info("========== 测试场景8: 稳定性测试 ==========");
        
        String testTopic = "/test/stability/simple";
        AtomicInteger executionCount = new AtomicInteger(0);
        int expectedCount = 10;
        CountDownLatch latch = new CountDownLatch(expectedCount);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("stability-test");
        
        // 订阅主题
        client.subscribe(testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 处理第 {} 条消息: {}", count, new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        ThreadUtil.sleep(500);
        
        // 发送10条消息
        log.info("📤 发送{}条消息", expectedCount);
        for (int i = 0; i < expectedCount; i++) {
            client.publishObject(testTopic, "消息-" + i);
            log.info("已发送 {}/{} 条", i + 1, expectedCount);
            ThreadUtil.sleep(500);
        }
        
        // 等待所有消息处理完成
        boolean allProcessed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(allProcessed, "所有消息应该在超时前处理完成");
        
        // 等待可能的额外执行
        ThreadUtil.sleep(2000);
        
        int finalExecutionCount = executionCount.get();
        
        log.info("✅ 发送消息数: {}", expectedCount);
        log.info("✅ 执行次数: {}", finalExecutionCount);
        
        if (finalExecutionCount != expectedCount) {
            log.error("❌ 测试失败！预期执行{}次，实际执行{}次，差异：{}", 
                    expectedCount, finalExecutionCount, finalExecutionCount - expectedCount);
        } else {
            log.info("✅ 测试通过！");
        }
        
        assertEquals(expectedCount, finalExecutionCount, 
                "稳定性测试：发送" + expectedCount + "条消息，应该执行" + expectedCount + "次");
    }
}

