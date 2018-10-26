package com.jbm.sample;

import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class MqttSampleService {

	@Bean(name = "mqttInputChannel")
	public MessageChannel mqttInputChannel() {
		return new DirectChannel();
	}

	@Bean(name = "mqttOutboundChannel")
	public MessageChannel mqttOutboundChannel() {
		return new DirectChannel();
	}

	@Bean(name = "errorChannel")
	public MessageChannel errorChannel() {
		return new DirectChannel();
	}
}
