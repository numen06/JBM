package jbm.framework.boot.autoconfigure.mqtt;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class RealMqttPahoClientFactory extends DefaultMqttPahoClientFactory {
	private final static Logger logger = LoggerFactory.getLogger(RealMqttPahoClientFactory.class);

	private MqttConnectProperties mqttConnectProperties;

	private LoadingCache<String, SimpleMqttPahoMessageHandler> mqttPahoClientCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES)
		.build(new CacheLoader<String, SimpleMqttPahoMessageHandler>() {
			@Override
			public SimpleMqttPahoMessageHandler load(String key) throws Exception {
				KeySerialization keySerialization = JSON.parseObject(key, KeySerialization.class);
				SimpleMqttPahoMessageHandler messageHandler = new SimpleMqttPahoMessageHandler(keySerialization.getClientId(), mqttPahoClientFactory());
				messageHandler.setAsync(false);
				messageHandler.setDefaultTopic(keySerialization.getDefaultTopic());
				messageHandler.setDefaultQos(0);
				messageHandler.setConverter(new DefaultPahoMessageConverter());
//				return  new SimpleMqttPahoMessageHandler(messageHandler);
				return messageHandler;
			}
		});

	public RealMqttPahoClientFactory(MqttConnectProperties mqttConnectProperties) {
		super();
		this.mqttConnectProperties = mqttConnectProperties;
	}

	private MqttPahoClientFactory mqttPahoClientFactory() {
		return this;
	}

	

	@Override
	public MqttConnectOptions getConnectionOptions() {
		MqttConnectOptions mqttConnectOptions = super.getConnectionOptions();
		mqttConnectOptions.setServerURIs(StringUtils.split(mqttConnectProperties.getUrl(), ","));
		mqttConnectOptions.setUserName(mqttConnectProperties.getUsername());
		mqttConnectOptions.setPassword(mqttConnectProperties.getPassword().toCharArray());
		mqttConnectOptions.setConnectionTimeout(mqttConnectProperties.getConnectionTimeout());
		mqttConnectOptions.setKeepAliveInterval(mqttConnectProperties.getKeepAliveInterval());
		return mqttConnectOptions;
	}

	public IMqttClient getClientInstance(String clientId) throws MqttException {
		return super.getClientInstance(mqttConnectProperties.getUrl(), clientId);
	}

	public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) throws MqttException {
		return super.getAsyncClientInstance(mqttConnectProperties.getUrl(), clientId);
	}

	public SimpleMqttPahoMessageHandler cteateSimpleMqttPahoMessageHandler() {
		return cteateSimpleMqttPahoMessageHandler(mqttConnectProperties.getClientId(), MqttHeaders.TOPIC);
	}

	public SimpleMqttPahoMessageHandler cteateSimpleMqttPahoMessageHandler(String defaultTopic) {
		return cteateSimpleMqttPahoMessageHandler(mqttConnectProperties.getClientId(), defaultTopic);
	}

	public SimpleMqttPahoMessageHandler cteateSimpleMqttPahoMessageHandler(String clientId, String defaultTopic) {
		logger.info("create client:{}", clientId);
		KeySerialization keySerialization = new KeySerialization(clientId, defaultTopic);
		String key = JSON.toJSONString(keySerialization);
		try {
			return mqttPahoClientCache.get(key);
		} catch (ExecutionException e) {
			return null;
		}
	}
}
