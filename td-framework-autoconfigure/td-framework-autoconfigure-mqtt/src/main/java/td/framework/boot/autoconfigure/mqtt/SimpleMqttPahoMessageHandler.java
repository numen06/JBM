package td.framework.boot.autoconfigure.mqtt;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;

import com.google.common.collect.Maps;

public class SimpleMqttPahoMessageHandler extends MqttPahoMessageHandler {

	public SimpleMqttPahoMessageHandler(String clientId, MqttPahoClientFactory clientFactory) {
		super(clientId, clientFactory);
	}

	@Override
	protected void publish(String topic, Object mqttMessage, Message<?> message) throws Exception {
		super.publish(topic, mqttMessage, message);
	}

	public void publish(String topic, Object obj, int qos) throws Exception {
		Message<Object> message = new Message<Object>() {
			@Override
			public Object getPayload() {
				return obj;
			}

			@Override
			public MessageHeaders getHeaders() {
				return new MessageHeaders(Maps.newHashMap());
			}
		};
		if (topic == null) {

			throw new MessageHandlingException(message, "No '" + MqttHeaders.TOPIC + "' header and no default topic defined");
		}
		Object mqttMessage = this.getConverter().fromMessage(message, Object.class);
		super.publish(topic, mqttMessage, message);
	}

}
