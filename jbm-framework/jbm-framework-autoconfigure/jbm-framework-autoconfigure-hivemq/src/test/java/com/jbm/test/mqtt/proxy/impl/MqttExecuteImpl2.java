package com.jbm.test.mqtt.proxy.impl;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 第二个 MqttMapper 实现，用于测试多个类监听同一个 topic
 */
@MqttMapper(clientId = "test")
@Service
@Slf4j
public class MqttExecuteImpl2 {

    /**
     * 监听与 MqttExecuteImpl 相同的 topic: /test/from
     * 测试：多个类可以监听同一个 topic，收到1条消息时，每个类的方法都执行1次
     */
    @MqttRequest(fromTopic = "/test/from")
    public void handleFromInClass2(String msg) {
        log.info("✅ [MqttExecuteImpl2.handleFromInClass2] 收到消息: {}", msg);
    }

    /**
     * 监听另一个相同的 topic
     */
    @MqttRequest(fromTopic = "/test/to")
    public void handleToInClass2(String msg) {
        log.info("✅ [MqttExecuteImpl2.handleToInClass2] 收到消息: {}", msg);
    }
}

