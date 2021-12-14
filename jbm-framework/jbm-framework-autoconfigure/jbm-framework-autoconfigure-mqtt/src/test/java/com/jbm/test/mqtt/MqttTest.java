package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.fusesource.mqtt.client.QoS;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.SimpleMqttPahoMessageHandler;

@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(classes = {MqttAutoConfiguration.class})
@Slf4j
public class MqttTest {
    @Autowired
    private RealMqttPahoClientFactory mqttPahoClientFactory;

    private IMqttClient mqttClient;

    @Before
    public void testClient() throws Exception {
//		messageHandler.publish("spm_alarm_in", "tewt", 1);
        mqttClient = mqttPahoClientFactory.getClientInstance();
    }


    @Test
    public void testPublish() throws MqttException {
        mqttClient.subscribe("test", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                log.info("topic:{},body:{}", topic, JSON.parse(message.getPayload()));
            }
        });
        while (true) {
            try {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(JSON.toJSONBytes("test"));
                while (true) {
                    mqttClient.publish("test", mqttMessage);
                    ThreadUtil.safeSleep(1000);
                }
            } catch (Exception e) {
                log.error("链接发生错误,休息一下重连");
                ThreadUtil.safeSleep(1000);
            }
        }

    }
}
