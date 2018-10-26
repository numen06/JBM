package com.jbm.sample.mqtt;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.Subscribe;
import com.jbm.sample.data.SimulationDataService;

import jbm.framework.boot.autoconfigure.mqtt.RealMqttPahoClientFactory;
import jbm.framework.boot.autoconfigure.mqtt.SimpleMqttPahoMessageHandler;

@Service
public class MqttDataService {
	private final static Logger logger = LoggerFactory.getLogger(MqttDataService.class);

	private String topic = "/yd";

	@Value("${spring.mqtt.provider.clientId:testProvider}")
	private String clientId;
	
	@Autowired(required = false)
	private SimpleMqttPahoMessageHandler messageHandler;

	@Autowired
	private RealMqttPahoClientFactory client;

	@PostConstruct
	public void init() {
		SimulationDataService.eventBus.register(this);
		messageHandler = client.cteateSimpleMqttPahoMessageHandler(clientId, topic);
	}

	@Subscribe
	public void revData(JSONObject data) {
		try {
			if (messageHandler != null) {
				messageHandler.publish("/yd/psn/" + data.getString("psn"), data.toJSONString(), 1);
			}
//			logger.info(data.toJSONString());
		} catch (Exception e) {
			logger.error("mqtt发送错误!messageHandler:" + messageHandler, e);
		}
	}

}
