package jbm.framework.boot.autoconfigure.mqtt.client;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import jbm.framework.boot.autoconfigure.mqtt.MqttConnectProperties;
import jbm.framework.boot.autoconfigure.mqtt.callback.SimpleMqttCallback;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Map;

@Slf4j
public class SimpleMqttClient extends SimpleMqttCallback {

    private final Map<String, IMqttMessageListener> topicFilterMap = Maps.newConcurrentMap();

    private MqttConnectProperties mqttConnectProperties;

    public SimpleMqttClient(IMqttClient mqttClient) {
        super(mqttClient);
        if (!mqttClient.isConnected()) {
            log.error("MQTT客户端[{}]未提前连接", mqttClient.getClientId());
        }
        mqttClient.setCallback(this);
        log.info("MQTT简单客户端[{}]启动成功", mqttClient.getClientId());
    }


    public SimpleMqttClient(IMqttClient mqttClient, MqttConnectProperties mqttConnectProperties) {
        super(mqttClient);
        mqttClient.setCallback(this);
        this.mqttConnectProperties = mqttConnectProperties;
        try {
            connect();
        } catch (Exception e) {
            connectionLost(e);
        }
        log.info("simple client [{}] start", mqttClient.getClientId());
    }


    public IMqttClient getClient() {
        return super.mqttClient;
    }

    public void reSubscribe() throws MqttException {
        for (String topic : topicFilterMap.keySet()) {
            this.subscribe(topic, topicFilterMap.get(topic));
        }
    }

    public void subscribe(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
        this.mqttClient.subscribe(topicFilter, messageListener);
        this.topicFilterMap.put(topicFilter, messageListener);
    }

    public IMqttToken subscribeWithResponse(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
        IMqttToken mqttToken = this.mqttClient.subscribeWithResponse(topicFilter, messageListener);
        this.topicFilterMap.put(topicFilter, messageListener);
        return mqttToken;
    }

    public void publish(String topic, MqttMessage message) throws MqttException {
        this.mqttClient.publish(topic, message);
    }

    /**
     * 发送消息
     *
     * @param topic
     * @param message
     * @throws MqttException
     */
    public void publishObject(String topic, Object message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage();
        if (message instanceof String) {
            mqttMessage.setPayload(StrUtil.bytes(message.toString()));
        } else {
            mqttMessage.setPayload(JSON.toJSONBytes(message));
        }
        mqttMessage.setQos(1);
        this.mqttClient.publish(topic, mqttMessage);
    }


    public boolean isConnected() {
        return this.mqttClient.isConnected();
    }

    public void connect() throws MqttException {
        if (!this.mqttClient.isConnected()) {
            this.mqttClient.connect(mqttConnectProperties.toMqttConnectOptions());
        }
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
                if (throwable == null) {
                    log.info("客户端[{}]，准备链接", mqttClient.getClientId());
                } else {
                    log.warn("客户端[{}]错误，触发重连", mqttClient.getClientId(), throwable);
                }
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
