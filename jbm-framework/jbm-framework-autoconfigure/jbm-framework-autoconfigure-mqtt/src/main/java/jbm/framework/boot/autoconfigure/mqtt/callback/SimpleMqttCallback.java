package jbm.framework.boot.autoconfigure.mqtt.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Slf4j
public class SimpleMqttCallback implements MqttCallbackExtended {

    private final IMqttClient client;

    public SimpleMqttCallback(IMqttClient client) {
        this.client = client;
    }

    @Override
    public void connectComplete(boolean b, String s) {
        log.info("MQTT Clint:[{}]连接成功", client.getClientId());
    }


    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT Clint:[{}]连接丢失", client.getClientId(), throwable);
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        log.info("接收到主题[{}]的消息", topic);
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.info("发送主体[{}]的消息成功", iMqttDeliveryToken.getTopics());
    }
}
