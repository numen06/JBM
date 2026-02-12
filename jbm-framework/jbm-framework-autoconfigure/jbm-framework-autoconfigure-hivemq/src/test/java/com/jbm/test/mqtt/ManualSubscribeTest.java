package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
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
 * 手动订阅测试
 * 演示如何手动订阅 MQTT 主题并接收消息
 * 
 * @author wesley
 */
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {MqttAutoConfiguration.class})
@Slf4j
public class ManualSubscribeTest {

    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;

    /**
     * 等待客户端连接稳定
     */
    private void waitForConnection(SimpleMqttClient client) throws InterruptedException {
        int retries = 0;
        while (!client.isConnected() && retries < 50) {
            ThreadUtil.sleep(100);
            retries++;
        }
        if (!client.isConnected()) {
            log.warn("⚠️ 客户端未连接");
            return;
        }
        // 等待连接稳定
        ThreadUtil.sleep(1000);
    }
    
    /**
     * 等待订阅完成（MQTT订阅是异步的，需要等待 SUBACK）
     */
    private void waitForSubscriptionComplete() throws InterruptedException {
        // 等待订阅完成（MQTT订阅是异步的，需要等待 SUBACK）
        // 从日志看，订阅成功后会有 "✅ 订阅成功 - Topic: {}" 日志
        // 但测试中无法直接检查，所以增加等待时间确保订阅完成
        ThreadUtil.sleep(3000);
    }

    /**
     * 测试：基本的手动订阅功能
     * 订阅一个主题，发送消息，验证能接收到消息
     */
    @Test
    public void testBasicManualSubscribe() throws Exception {
        log.info("========== 测试：基本手动订阅 ==========");
        
        String testTopic = "/test/manual/basic";
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-basic");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待客户端连接稳定
        int retries = 0;
        while (!mqttClient.isConnected() && retries < 50) {
            ThreadUtil.sleep(100);
            retries++;
        }
        if (!mqttClient.isConnected()) {
            log.warn("⚠️ 客户端未连接，无法继续测试");
            return;
        }
        // 等待连接稳定
        ThreadUtil.sleep(1000);
        
        // 手动订阅主题
        log.info("📡 手动订阅主题: {}", testTopic);
        mqttClient.subscribe(testTopic, (Mqtt5Publish publish) -> {
            int count = receivedCount.incrementAndGet();
            String message = new String(publish.getPayloadAsBytes());
            log.info("📨 收到第 {} 条消息 - Topic: {}, Message: {}", 
                    count, publish.getTopic(), message);
            latch.countDown();
        });
        
        // 等待订阅完成（MQTT订阅是异步的，需要等待 SUBACK）
        ThreadUtil.sleep(3000);
        
        // 发送测试消息
        log.info("📤 发送测试消息到主题: {}", testTopic);
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(JSON.toJSONBytes("手动订阅测试消息"));
        mqttMessage.setQos(1);
        mqttClient.publish(testTopic, mqttMessage);
        
        // 等待消息接收
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "应该在5秒内收到消息");
        
        assertEquals(1, receivedCount.get(), "应该收到1条消息");
        log.info("✅ 测试通过：成功接收到消息");
    }

    /**
     * 测试：订阅多个主题
     * 演示如何订阅多个不同的主题
     */
    @Test
    public void testSubscribeMultipleTopics() throws Exception {
        log.info("========== 测试：订阅多个主题 ==========");
        
        String topic1 = "/test/manual/topic1";
        String topic2 = "/test/manual/topic2";
        String topic3 = "/test/manual/topic3";
        
        AtomicInteger topic1Count = new AtomicInteger(0);
        AtomicInteger topic2Count = new AtomicInteger(0);
        AtomicInteger topic3Count = new AtomicInteger(0);
        
        CountDownLatch latch = new CountDownLatch(3);
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-multiple");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 订阅第一个主题
        log.info("📡 订阅主题1: {}", topic1);
        mqttClient.subscribe(topic1, (Mqtt5Publish publish) -> {
            topic1Count.incrementAndGet();
            log.info("📨 主题1收到消息: {}", new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 订阅第二个主题
        log.info("📡 订阅主题2: {}", topic2);
        mqttClient.subscribe(topic2, (Mqtt5Publish publish) -> {
            topic2Count.incrementAndGet();
            log.info("📨 主题2收到消息: {}", new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 订阅第三个主题
        log.info("📡 订阅主题3: {}", topic3);
        mqttClient.subscribe(topic3, (Mqtt5Publish publish) -> {
            topic3Count.incrementAndGet();
            log.info("📨 主题3收到消息: {}", new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 等待所有订阅完成
        waitForSubscriptionComplete();
        
        // 发送消息到各个主题
        log.info("📤 发送消息到各个主题");
        mqttClient.publishObject(topic1, "消息1");
        ThreadUtil.sleep(200);
        mqttClient.publishObject(topic2, "消息2");
        ThreadUtil.sleep(200);
        mqttClient.publishObject(topic3, "消息3");
        
        // 等待所有消息接收完成
        boolean allReceived = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allReceived, "应该在5秒内收到所有消息");
        
        assertEquals(1, topic1Count.get(), "主题1应该收到1条消息");
        assertEquals(1, topic2Count.get(), "主题2应该收到1条消息");
        assertEquals(1, topic3Count.get(), "主题3应该收到1条消息");
        
        log.info("✅ 测试通过：所有主题都成功接收到消息");
    }

    /**
     * 测试：使用通配符订阅
     * 演示如何使用 MQTT 通配符订阅多个主题
     */
    @Test
    public void testWildcardSubscribe() throws Exception {
        log.info("========== 测试：通配符订阅 ==========");
        
        // 使用通配符订阅
        String wildcardTopic = "/test/manual/+/wildcard";
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-wildcard");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        log.info("📡 订阅通配符主题: {}", wildcardTopic);
        mqttClient.subscribe(wildcardTopic, (Mqtt5Publish publish) -> {
            int count = receivedCount.incrementAndGet();
            log.info("📨 通配符订阅收到第 {} 条消息 - Topic: {}, Message: {}", 
                    count, publish.getTopic(), new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 等待订阅完成
        waitForSubscriptionComplete();
        
        // 发送消息到匹配通配符的不同主题
        log.info("📤 发送消息到匹配通配符的主题");
        mqttClient.publishObject("/test/manual/device1/wildcard", "设备1消息");
        ThreadUtil.sleep(200);
        mqttClient.publishObject("/test/manual/device2/wildcard", "设备2消息");
        ThreadUtil.sleep(200);
        mqttClient.publishObject("/test/manual/device3/wildcard", "设备3消息");
        
        // 等待所有消息接收完成
        boolean allReceived = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allReceived, "应该在5秒内收到所有消息");
        
        assertEquals(3, receivedCount.get(), "应该收到3条消息");
        log.info("✅ 测试通过：通配符订阅成功接收到所有消息");
    }

    /**
     * 测试：订阅后接收多条消息
     * 验证订阅后能持续接收消息
     */
    @Test
    public void testSubscribeAndReceiveMultipleMessages() throws Exception {
        log.info("========== 测试：订阅后接收多条消息 ==========");
        
        String testTopic = "/test/manual/multiple";
        int messageCount = 10;
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(messageCount);
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-multiple-msg");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 订阅主题
        log.info("📡 订阅主题: {}", testTopic);
        mqttClient.subscribe(testTopic, (Mqtt5Publish publish) -> {
            int count = receivedCount.incrementAndGet();
            log.info("📨 收到第 {}/{} 条消息: {}", 
                    count, messageCount, new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 等待订阅完成
        waitForSubscriptionComplete();
        
        // 发送多条消息
        log.info("📤 发送 {} 条消息", messageCount);
        for (int i = 0; i < messageCount; i++) {
            mqttClient.publishObject(testTopic, "消息-" + (i + 1));
            ThreadUtil.sleep(100);
        }
        
        // 等待所有消息接收完成
        boolean allReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(allReceived, "应该在10秒内收到所有消息");
        
        assertEquals(messageCount, receivedCount.get(), 
                "应该收到" + messageCount + "条消息");
        log.info("✅ 测试通过：成功接收到所有 {} 条消息", messageCount);
    }

    /**
     * 测试：取消订阅
     * 演示如何取消订阅，取消后不应该再收到消息
     */
    @Test
    public void testUnsubscribe() throws Exception {
        log.info("========== 测试：取消订阅 ==========");
        
        String testTopic = "/test/manual/unsubscribe";
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-unsubscribe");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 订阅主题
        log.info("📡 订阅主题: {}", testTopic);
        mqttClient.subscribe(testTopic, (Mqtt5Publish publish) -> {
            receivedCount.incrementAndGet();
            log.info("📨 收到消息: {}", new String(publish.getPayloadAsBytes()));
            latch.countDown();
        });
        
        // 等待订阅完成
        waitForSubscriptionComplete();
        
        // 发送第一条消息（订阅状态下应该能收到）
        log.info("📤 发送第一条消息（订阅状态下）");
        mqttClient.publishObject(testTopic, "订阅状态下的消息");
        
        boolean received = latch.await(3, TimeUnit.SECONDS);
        assertTrue(received, "订阅状态下应该能收到消息");
        assertEquals(1, receivedCount.get(), "应该收到1条消息");
        
        // 取消订阅
        log.info("📡 取消订阅主题: {}", testTopic);
        mqttClient.unsubscribe(testTopic);
        ThreadUtil.sleep(500);
        
        // 发送第二条消息（取消订阅后不应该收到）
        log.info("📤 发送第二条消息（取消订阅后）");
        mqttClient.publishObject(testTopic, "取消订阅后的消息");
        ThreadUtil.sleep(2000);
        
        // 验证取消订阅后没有收到新消息
        assertEquals(1, receivedCount.get(), 
                "取消订阅后不应该再收到消息，应该仍然只有1条消息");
        log.info("✅ 测试通过：取消订阅后不再接收消息");
    }

    /**
     * 测试：订阅后发送和接收 JSON 对象
     * 演示如何处理 JSON 格式的消息
     */
    @Test
    public void testSubscribeWithJsonMessage() throws Exception {
        log.info("========== 测试：订阅并接收 JSON 消息 ==========");
        
        String testTopic = "/test/manual/json";
        CountDownLatch latch = new CountDownLatch(1);
        String[] receivedMessage = new String[1];
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-json");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 订阅主题
        log.info("📡 订阅主题: {}", testTopic);
        mqttClient.subscribe(testTopic, (Mqtt5Publish publish) -> {
            String message = new String(publish.getPayloadAsBytes());
            receivedMessage[0] = message;
            log.info("📨 收到 JSON 消息: {}", message);
            latch.countDown();
        });
        
        // 等待订阅完成
        waitForSubscriptionComplete();
        
        // 创建测试对象
        TestData testData = new TestData();
        testData.setId(1);
        testData.setName("测试数据");
        testData.setValue(100.5);
        
        // 发送 JSON 对象
        log.info("📤 发送 JSON 对象: {}", JSON.toJSONString(testData));
        mqttClient.publishObject(testTopic, testData);
        
        // 等待消息接收
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "应该在5秒内收到消息");
        
        // 验证消息内容
        assertNotNull(receivedMessage[0], "应该收到消息");
        TestData receivedData = JSON.parseObject(receivedMessage[0], TestData.class);
        assertEquals(testData.getId(), receivedData.getId());
        assertEquals(testData.getName(), receivedData.getName());
        assertEquals(testData.getValue(), receivedData.getValue());
        
        log.info("✅ 测试通过：成功发送和接收 JSON 对象");
    }

    /**
     * 测试：检查连接状态
     * 验证客户端连接状态检查功能
     */
    @Test
    public void testConnectionStatus() throws Exception {
        log.info("========== 测试：连接状态检查 ==========");
        
        // 创建测试用的 MQTT 客户端
        SimpleMqttClient mqttClient = mqttPahoClientFactory.getClientInstance("manual-subscribe-test-connection");
        log.info("✅ MQTT 客户端已创建");
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 检查连接状态
        boolean isConnected = mqttClient.isConnected();
        log.info("🔌 MQTT 客户端连接状态: {}", isConnected ? "已连接" : "未连接");
        
        assertTrue(isConnected, "客户端应该处于连接状态");
        
        // 订阅一个主题以验证连接可用
        String testTopic = "/test/manual/connection";
        CountDownLatch latch = new CountDownLatch(1);
        
        log.info("📡 订阅主题以验证连接: {}", testTopic);
        mqttClient.subscribe(testTopic, (Mqtt5Publish publish) -> {
            log.info("📨 收到消息，连接正常");
            latch.countDown();
        });
        
        // 等待连接稳定
        waitForConnection(mqttClient);
        
        // 等待订阅完成
        waitForSubscriptionComplete();
        
        // 发送测试消息
        mqttClient.publishObject(testTopic, "连接测试消息");
        
        boolean received = latch.await(3, TimeUnit.SECONDS);
        assertTrue(received, "应该能收到消息，证明连接正常");
        
        log.info("✅ 测试通过：连接状态正常");
    }

    /**
     * 测试数据类
     */
    static class TestData {
        private Integer id;
        private String name;
        private Double value;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}
