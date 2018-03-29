package com.td.sample;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.td.sample.MqttProvider.MessageGateway;

import td.framework.boot.autoconfigure.mqtt.SimpleMqttPahoMessageHandler;

@Service
public class MqttSendService {
	Logger logger = LoggerFactory.getLogger(MqttSendService.class);
	@Autowired
	private MessageGateway gateway;

	@Value("${runtime:1}")
	private Integer runtime;

	@Autowired
	private SimpleMqttPahoMessageHandler messageHandler;

	@Scheduled(cron = "${scheduled:0/1 * * * * ?}")
	public void init() {
		try {
			// gateway.sendToMqtt(UUID.randomUUID().toString());
			messageHandler.publish("test", UUID.randomUUID().toString(), 2);
			// Thread.sleep(1000);
			for (int i = 0; i < runtime; i++) {
				gateway.sendToMqtt(JSON.toJSONString(new Greeting(System.currentTimeMillis() + "天才张" + "[" + i + "]")));
			}
		} catch (MessageDeliveryException e) {
			logger.error("发送异常", e);
		} catch (Exception e) {
			logger.error("没有消费者", e);
		}
	}

}
