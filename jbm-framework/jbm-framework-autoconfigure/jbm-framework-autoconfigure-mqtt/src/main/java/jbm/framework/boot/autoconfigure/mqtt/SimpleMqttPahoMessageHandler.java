package jbm.framework.boot.autoconfigure.mqtt;

import com.google.common.collect.Maps;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;

import java.util.Map;

public class SimpleMqttPahoMessageHandler extends MqttPahoMessageHandler {


    public SimpleMqttPahoMessageHandler(String url, String clientId, MqttPahoClientFactory clientFactory) {
        super(url, clientId, clientFactory);
    }

    public SimpleMqttPahoMessageHandler(String clientId, MqttPahoClientFactory clientFactory) {
        super((String) null, clientId, clientFactory);
    }

    public SimpleMqttPahoMessageHandler(String url, String clientId) {
        this(url, clientId, new DefaultMqttPahoClientFactory());
    }

    public void publish(String topic, Object obj, int qos) throws Exception {
        Message<Object> message = new Message<Object>() {
            @Override
            public Object getPayload() {
                return obj;
            }

            @Override
            public MessageHeaders getHeaders() {
                Map<String, Object> header = Maps.newHashMap();
                header.put("mqtt_topic", topic);
                return new MessageHeaders(header);
            }
        };
        if (topic == null) {
            throw new MessageHandlingException(message,
                    "No '" + MqttHeaders.TOPIC + "' header and no default topic defined");
        }
        super.handleMessage(message);
    }

}
