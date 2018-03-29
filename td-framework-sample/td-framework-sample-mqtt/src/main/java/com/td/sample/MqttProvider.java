package com.td.sample;

import java.util.Date;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

import td.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import td.framework.boot.autoconfigure.mqtt.SimpleMqttPahoMessageHandler;

@Configuration
public class MqttProvider {

	@Value("${spring.mqtt.provider.topic:/topic/queue/application/1483602569655}")
	private String topic;
	@Value("${spring.mqtt.provider.clientId:testProvider}")
	private String clientId;

	@Autowired
	private RealMqttPahoClientFactory client;

	@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
	public interface MessageGateway {
		@Gateway(replyTimeout = 1, requestTimeout = 30)
		void sendToMqtt(String data) throws Exception;

		void sendDateToMqtt(Date data);

		void sendGreeting(Greeting data);
	}

	@Bean
	@ServiceActivator(inputChannel = "mqttOutboundChannel")
	public MessageHandler mqttOutbound() {
		// MqttPahoMessageHandler messageHandler = new
		// MqttPahoMessageHandler(clientId, client);
		// messageHandler.setAsync(true);
		// messageHandler.setDefaultTopic(topic);
		// messageHandler.setDefaultQos(2);
		SimpleMqttPahoMessageHandler messageHandler = client.cteateSimpleMqttPahoMessageHandler(clientId, topic);
		return messageHandler;
	}

}
