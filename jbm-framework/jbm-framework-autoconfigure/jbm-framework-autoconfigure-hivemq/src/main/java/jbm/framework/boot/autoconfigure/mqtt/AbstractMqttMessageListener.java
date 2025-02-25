package jbm.framework.boot.autoconfigure.mqtt;

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public abstract class AbstractMqttMessageListener implements IMqttMessageListener, Consumer<Mqtt5Publish> {
    /**
     * @param publish the input argument
     */
    @Override
    public void accept(Mqtt5Publish publish) {
        try {
            MqttMessage message = new MqttMessage(publish.getTopic().toString(), publish.getPayloadAsBytes());
            messageArrived(message.getTopic(), message);
        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }

    @Override
    public abstract void messageArrived(String topic, MqttMessage message) throws Exception;
}
