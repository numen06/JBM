package jbm.framework.boot.autoconfigure.mqtt.client;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import jbm.framework.boot.autoconfigure.mqtt.callback.SimpleMqttAsyncClientCallback;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Map;

@Slf4j
public class SimpleMqttAsyncClient extends SimpleMqttAsyncClientCallback {

    private Map<String, IMqttMessageListener> topicFilterMap = Maps.newConcurrentMap();

    public SimpleMqttAsyncClient(IMqttAsyncClient mqttAsyncClient) {
        super(mqttAsyncClient);
        mqttAsyncClient.setCallback(this);
        log.info("simple async client [{}] start", mqttClient.getClientId());
    }

    public IMqttAsyncClient getClient() {
        return super.mqttClient;
    }

    public void reSubscribe() throws MqttException {
        for (String topic : topicFilterMap.keySet()) {
            this.subscribe(topic, topicFilterMap.get(topic));
        }
    }

    public IMqttToken subscribe(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
        return this.subscribe(topicFilter, 1, messageListener);
    }

    public IMqttToken subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException {
        IMqttToken iMqttToken = this.mqttClient.subscribe(topicFilter, qos, messageListener);
        this.topicFilterMap.put(topicFilter, messageListener);
        return iMqttToken;
    }

    public void publish(String topic, MqttMessage message) throws MqttException {
        this.mqttClient.publish(topic, message);
    }

    public void publishObject(String topic, Object message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(JSON.toJSONBytes(message));
        mqttMessage.setQos(1);
        this.mqttClient.publish(topic, mqttMessage);
    }

    public boolean isConnected() {
        return this.mqttClient.isConnected();
    }

    public void connect() throws MqttException {
        if (!this.isConnected())
            this.mqttClient.connect();
    }

    public void disconnect() throws MqttException {
        this.mqttClient.disconnect();
    }


    public void unsubscribe(String topicFilter) throws MqttException {
        this.mqttClient.unsubscribe(topicFilter);
        this.topicFilterMap.remove(topicFilter);
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("MQTT Client:[{}]连接丢失", mqttClient.getClientId(), throwable);
        while (!mqttClient.isConnected()) {
            try {
                log.info("客户端[{}]出发重连", mqttClient.getClientId());
                mqttClient.connect();
            } catch (MqttException e) {
                log.error("reconnect error", e);
                ThreadUtil.safeSleep(3000);
            }
        }
        try {
            this.reSubscribe();
        } catch (Exception e) {
            log.error("reSubscribe error", e);
        }
    }


}
