package com.td.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@Configuration
public class MqttConsumer {

	@Value("${spring.mqtt.consumer.topic:test}")
	private String topic;
	@Value("${spring.mqtt.consumer.clientId:testConsumer}")
	private String clientId;


	@Autowired
	private MqttPahoClientFactory client;
	@Autowired
	@Qualifier("mqttInputChannel")
	private MessageChannel messageChannel;

	@Autowired
	@Qualifier("errorChannel")
	private MessageChannel errorChannel;

	@Bean
	public MessageProducer inbound() {
		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(clientId, client, topic);
		adapter.setCompletionTimeout(5000);
		adapter.setConverter(new DefaultPahoMessageConverter());
		adapter.setQos(2);
		adapter.setOutputChannel(messageChannel);
		adapter.setErrorChannel(errorChannel);
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "errorChannel")
	public MessageHandler errorhandler() {
		return new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				System.err.println(message.getPayload());
			}
		};
	}

	@Bean
	@ServiceActivator(inputChannel = "mqttInputChannel")
	public MessageHandler inputhandler() {
		return new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				System.err.println(message.getPayload());
			}

		};
	}

}
