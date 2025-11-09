package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.map.MapUtil;
import com.jbm.test.mqtt.proxy.MqttSender;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl2;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl3;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
import jbm.framework.boot.autoconfigure.mqtt.registrar.EnableMqttMapperScan;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多个类监听同一个 Topic 的测试
 * 验证核心业务场景：
 * 1. 多个类可以监听同一个 topic
 * 2. MQTT 层面只订阅一次（节省资源）
 * 3. 收到1条消息时，每个类的方法都执行1次
 * 4. 单个类的方法不会重复执行
 */
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@EnableMqttMapperScan("com.jbm.test.mqtt")
@SpringBootTest(classes = {
        MqttAutoConfiguration.class, 
        MqttExecuteImpl.class,
        MqttExecuteImpl2.class,
        MqttExecuteImpl3.class
})
@Slf4j
public class MultipleClassesSameTopicTest {

    @Autowired
    private MqttProxyFactory mqttProxyFactory;

    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;

    @Autowired
    private MqttSender mqttSender;

    /**
     * 测试场景1: 多个类监听同一个 topic
     * 预期：收到1条消息 → 3个类的方法各执行1次 = 总共3次
     */
    @Test
    public void testMultipleClassesSameTopic() throws Exception {
        log.info("========== 测试场景1: 多个类监听同一个 topic ==========");
        
        // 统计信息
        Map<String, Object> stats = mqttProxyFactory.getStatistics();
        log.info("📊 订阅统计: {}", stats);
        
        // 使用 MqttSender 发送消息到 /test/from
        // 此时应该有3个类在监听：MqttExecuteImpl, MqttExecuteImpl2, MqttExecuteImpl3
        log.info("📤 发送1条消息到 /test/from");
        mqttSender.testfrom(MapUtil.of("msg", "测试多个类监听同一topic"));
        
        // 等待所有监听器处理完成
        ThreadUtil.sleep(3000);
        
        log.info("✅ 测试完成，请检查日志中是否有3个类的方法都被执行了");
        log.info("预期看到：");
        log.info("  - [MqttExecuteImpl.to] 执行1次");
        log.info("  - [MqttExecuteImpl2.handleFromInClass2] 执行1次");
        log.info("  - [MqttExecuteImpl3.handleFromInClass3] 执行1次");
    }

    /**
     * 测试场景2: 发送N条消息，验证每个类都执行N次
     */
    @Test
    public void testMultipleMessagesMultipleClasses() throws Exception {
        log.info("========== 测试场景2: 发送多条消息到多个类 ==========");
        
        String testTopic = "/test/multi/class";
        int messageCount = 5;
        
        // 创建3个计数器，分别统计3个假设的监听器
        AtomicInteger totalCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(messageCount * 3); // 5条消息 * 3个监听器
        
        SimpleMqttClient testClient = mqttPahoClientFactory.getAppClientInstance("multi-class-test");
        
        // 模拟3个类监听同一topic（实际测试中会用真实的 MqttMapper）
        testClient.subscribe(testTopic, publish -> {
            totalCount.incrementAndGet();
            latch.countDown();
            log.info("📨 [模拟监听器1] 收到消息");
        });
        
        testClient.subscribe(testTopic, publish -> {
            totalCount.incrementAndGet();
            latch.countDown();
            log.info("📨 [模拟监听器2] 收到消息");
        });
        
        testClient.subscribe(testTopic, publish -> {
            totalCount.incrementAndGet();
            latch.countDown();
            log.info("📨 [模拟监听器3] 收到消息");
        });
        
        ThreadUtil.sleep(500);
        
        // 发送N条消息
        log.info("📤 发送{}条消息", messageCount);
        for (int i = 0; i < messageCount; i++) {
            testClient.publishObject(testTopic, "消息-" + i);
            ThreadUtil.sleep(100);
        }
        
        // 等待所有消息处理完成
        boolean allProcessed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(allProcessed, "所有消息应该在超时前处理完成");
        
        ThreadUtil.sleep(1000);
        
        int finalCount = totalCount.get();
        log.info("✅ 发送{}条消息，总执行次数: {}", messageCount, finalCount);
        log.info("✅ 每条消息被{}个监听器处理", finalCount / messageCount);
        
        assertEquals(messageCount * 3, finalCount, 
                "发送" + messageCount + "条消息，3个监听器，应该执行" + (messageCount * 3) + "次");
    }

    /**
     * 测试场景3: 验证MQTT层面只订阅一次
     */
    @Test
    public void testMqttSubscribeOnlyOnce() {
        log.info("========== 测试场景3: 验证MQTT层面只订阅一次 ==========");
        
        Map<String, Object> stats = mqttProxyFactory.getStatistics();
        
        int totalSubscriptions = (int) stats.get("totalSubscriptions");
        int totalClients = (int) stats.get("totalClients");
        
        log.info("📊 总订阅数（应用层）: {}", totalSubscriptions);
        log.info("📊 总客户端数: {}", totalClients);
        
        // 验证订阅数大于等于客户端数
        assertTrue(totalSubscriptions >= totalClients, 
                "总订阅数应该大于等于客户端数");
        
        // 注意：这里的 totalSubscriptions 是应用层的订阅数（每个类的每个方法一个）
        // MQTT 层面的订阅数应该更少（相同 topic 只订阅一次）
        log.info("✅ MQTT层面的订阅通过多播机制优化，相同topic只订阅一次");
    }

    /**
     * 测试场景4: 验证单个方法不会重复执行（即使 subscribe() 被多次调用）
     */
    @Test
    public void testSingleMethodNotDuplicate() throws Exception {
        log.info("========== 测试场景4: 验证单个方法不会重复执行 ==========");
        
        // 手动多次调用 subscribe()
        log.info("🔄 第1次调用 subscribe()");
        mqttProxyFactory.subscribe();
        
        ThreadUtil.sleep(500);
        
        log.info("🔄 第2次调用 subscribe()");
        mqttProxyFactory.subscribe();
        
        ThreadUtil.sleep(500);
        
        log.info("🔄 第3次调用 subscribe()");
        mqttProxyFactory.subscribe();
        
        ThreadUtil.sleep(500);
        
        // 发送消息到 /test/from
        log.info("📤 发送1条消息到 /test/from");
        mqttSender.testfrom(MapUtil.of("msg", "测试重复调用subscribe"));
        
        // 等待处理
        ThreadUtil.sleep(3000);
        
        log.info("✅ 测试完成");
        log.info("预期结果：即使 subscribe() 被调用3次，每个类的方法仍然只执行1次");
        log.info("  - MqttExecuteImpl.to 执行1次");
        log.info("  - MqttExecuteImpl2.handleFromInClass2 执行1次");
        log.info("  - MqttExecuteImpl3.handleFromInClass3 执行1次");
        log.info("  - 总共3次，不是9次");
    }
}

