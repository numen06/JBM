package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的消息测试（不加载任何 MqttMapper，避免干扰）
 * 专门测试：发送N条消息，执行N次
 */
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {MqttAutoConfiguration.class})  // 不加载 MqttExecuteImpl
@Slf4j
public class SimpleMessageTest {

    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;

    private final String testTag = UUID.randomUUID().toString().substring(0, 8);

    private void subscribeReady(SimpleMqttClient client, String topic, Consumer<Mqtt5Publish> handler) throws Exception {
        int retries = 0;
        while (!client.isConnected() && retries < 50) {
            ThreadUtil.sleep(100);
            retries++;
        }
        assertTrue(client.isConnected(), "客户端应已连接");
        assertTrue(client.subscribeAndWait(topic, handler, 10, TimeUnit.SECONDS), "订阅应在10秒内完成");
    }

    /**
     * 测试：发送1条消息，执行1次
     */
    @Test
    public void testOneMessageOneExecution() throws Exception {
        log.info("========== 测试：发送1条消息，执行1次 ==========");
        
        String testTopic = "/test/simple/one";
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("simple-test-1-" + testTag);
        subscribeReady(client, testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 第 {} 次执行", count);
            latch.countDown();
        });
        
        // 发送1条消息
        log.info("📤 发送1条消息");
        client.publishObject(testTopic, "测试消息1");
        
        boolean processed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(processed, "消息应该被处理");
        
        // 等待可能的重复执行
        ThreadUtil.sleep(2000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: 1, 执行: {}", finalCount);
        
        assertEquals(1, finalCount, "发送1条消息，应该执行1次");
    }

    /**
     * 测试：发送5条消息，执行5次
     */
    @Test
    public void testFiveMessagesFiveExecutions() throws Exception {
        log.info("========== 测试：发送5条消息，执行5次 ==========");
        
        String testTopic = "/test/simple/five";
        AtomicInteger executionCount = new AtomicInteger(0);
        int messageCount = 5;
        CountDownLatch latch = new CountDownLatch(messageCount);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("simple-test-5-" + testTag);
        subscribeReady(client, testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 第 {} 次执行: {}", count, new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 发送5条消息
        log.info("📤 发送{}条消息", messageCount);
        for (int i = 0; i < messageCount; i++) {
            client.publishObject(testTopic, "消息-" + i);
            ThreadUtil.sleep(200);
        }
        
        boolean allProcessed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allProcessed, "所有消息应该被处理");
        
        // 等待可能的额外执行
        ThreadUtil.sleep(1000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: {}, 执行: {}", messageCount, finalCount);
        
        assertEquals(messageCount, finalCount, "发送" + messageCount + "条消息，应该执行" + messageCount + "次");
    }

    /**
     * 测试：长时间运行稳定性（简化版：10秒）
     */
    @Test
    public void testLongRunningStability() throws Exception {
        log.info("========== 测试：长时间运行稳定性（10秒）==========");
        
        String testTopic = "/test/simple/stability";
        AtomicInteger executionCount = new AtomicInteger(0);
        int expectedCount = 10;
        CountDownLatch latch = new CountDownLatch(expectedCount);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("simple-stability-test-" + testTag);
        subscribeReady(client, testTopic, publish -> {
            int count = executionCount.incrementAndGet();
            log.info("📨 处理第 {} 条消息: {}", count, new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 发送10条消息，每秒1条
        log.info("📤 开始测试，发送{}条消息，每秒1条", expectedCount);
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < expectedCount; i++) {
            client.publishObject(testTopic, "消息-" + i);
            log.info("已发送 {}/{} 条消息", i + 1, expectedCount);
            ThreadUtil.sleep(1000);
        }
        
        // 等待所有消息处理完成
        boolean allProcessed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allProcessed, "所有消息应该在超时前处理完成");
        
        // 再等待一段时间，确认没有额外的执行
        ThreadUtil.sleep(2000);
        
        int finalExecutionCount = executionCount.get();
        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        
        log.info("✅ 总耗时: {}秒", totalTime);
        log.info("✅ 发送消息数: {}", expectedCount);
        log.info("✅ 执行次数: {}", finalExecutionCount);
        
        if (finalExecutionCount != expectedCount) {
            log.error("❌ 测试失败！预期执行{}次，实际执行{}次，差异：{}", 
                    expectedCount, finalExecutionCount, finalExecutionCount - expectedCount);
        } else {
            log.info("✅ 测试通过！发送和执行次数完全一致");
        }
        
        assertEquals(expectedCount, finalExecutionCount, 
                "长时间运行：发送" + expectedCount + "条消息，应该执行" + expectedCount + "次，实际执行" + finalExecutionCount + "次");
    }

    /**
     * 测试：快速发送10条不同消息
     */
    @Test
    public void testRapidDifferentMessages() throws Exception {
        log.info("========== 测试：快速发送10条不同消息 ==========");
        
        String testTopic = "/test/simple/rapid";
        AtomicInteger executionCount = new AtomicInteger(0);
        int messageCount = 10;
        CountDownLatch latch = new CountDownLatch(messageCount);
        
        SimpleMqttClient client = mqttPahoClientFactory.getAppClientInstance("simple-rapid-test-" + testTag);
        subscribeReady(client, testTopic, publish -> {
            executionCount.incrementAndGet();
            latch.countDown();
        });
        
        // 快速发送10条不同消息
        log.info("📤 快速发送{}条消息", messageCount);
        for (int i = 0; i < messageCount; i++) {
            client.publishObject(testTopic, "消息-" + i);
            ThreadUtil.sleep(50);  // 很短的间隔
        }
        
        boolean allProcessed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allProcessed, "所有消息应该被处理");
        
        ThreadUtil.sleep(1000);
        
        int finalCount = executionCount.get();
        log.info("✅ 发送: {}, 执行: {}", messageCount, finalCount);
        
        assertEquals(messageCount, finalCount, "发送" + messageCount + "条消息，应该执行" + messageCount + "次");
    }
}

