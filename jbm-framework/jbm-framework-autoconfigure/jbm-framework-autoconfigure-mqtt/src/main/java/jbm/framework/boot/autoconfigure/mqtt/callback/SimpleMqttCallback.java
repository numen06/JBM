package jbm.framework.boot.autoconfigure.mqtt.callback;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

@Slf4j
public class SimpleMqttCallback implements MqttCallbackExtended {

    protected final IMqttClient mqttClient;

    public SimpleMqttCallback(IMqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT Client:[{}]连接成功", mqttClient.getClientId());
    }


    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT Client:[{}]连接丢失", mqttClient.getClientId(), throwable);
        while (!mqttClient.isConnected()) {
            try {
                log.info("客户端[{}]触发重连", mqttClient.getClientId());
                mqttClient.reconnect();
                ThreadUtil.safeSleep(3000);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        log.debug("接收到主题[{}]的消息", topic);
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.debug("发送主体[{}]的消息成功", iMqttDeliveryToken.getTopics());
    }

}
