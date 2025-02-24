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
            messageArrived(publish.getTopic().toString(), new MqttMessage());
        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }

    public abstract void messageArrived(String topic, MqttMessage message) throws Exception;
}
