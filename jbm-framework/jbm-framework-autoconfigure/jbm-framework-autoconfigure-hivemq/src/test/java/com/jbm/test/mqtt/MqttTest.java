package com.jbm.test.mqtt;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {MqttAutoConfiguration.class})
@Slf4j
public class MqttTest {
    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;

    private SimpleMqttClient mqttClient;

    @BeforeEach
    public void testClient() throws Exception {
        String clientId = "mqtt-test-" + UUID.randomUUID().toString().substring(0, 8);
        mqttClient = mqttPahoClientFactory.getClientInstance(clientId);
    }


    @Test
    public void testPublish() throws Exception {
        int retries = 0;
        while (!mqttClient.isConnected() && retries < 50) {
            ThreadUtil.sleep(100);
            retries++;
        }
        mqttClient.subscribeAndWait("test", (publish) -> {
            log.info("接受topic:{},body:{}", publish.getTopic(), JSON.parse(publish.getPayloadAsBytes()));
        }, 10, TimeUnit.SECONDS);
        for (int i = 0; i < 5; i++) {
            try {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(JSON.toJSONBytes("我是测试消息-" + DateUtil.now()));
                mqttClient.publish("test", mqttMessage);
                ThreadUtil.safeSleep(1000);
            } catch (Exception e) {
                log.error("链接发生错误,休息一下重连");
                ThreadUtil.safeSleep(1000);
            }
        }

    }
}
