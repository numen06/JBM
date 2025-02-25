package jbm.framework.boot.autoconfigure.mqtt;

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;

import java.util.function.Consumer;

/**
 * @author wesley
 */
public interface IMqttMessageListener extends Consumer<Mqtt5Publish> {

    void messageArrived(String topic, MqttMessage message) throws Exception;
}
