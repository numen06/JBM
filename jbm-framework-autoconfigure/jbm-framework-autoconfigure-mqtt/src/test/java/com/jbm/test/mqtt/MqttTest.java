package com.jbm.test.mqtt;

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
@SpringBootTest(classes = { MqttAutoConfiguration.class })
public class MqttTest {
	@Autowired
	private RealMqttPahoClientFactory mqttPahoClientFactory;

	@Test
	public void test() throws Exception {
		SimpleMqttPahoMessageHandler messageHandler = mqttPahoClientFactory.cteateSimpleMqttPahoMessageHandler("spm_alarm_in");
		messageHandler.publish("spm_alarm_in", "tewt", 1);
	}
}
