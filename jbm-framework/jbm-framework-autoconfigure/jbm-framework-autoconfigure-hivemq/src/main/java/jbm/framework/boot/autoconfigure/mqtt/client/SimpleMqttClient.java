package jbm.framework.boot.autoconfigure.mqtt.client;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.jbm.util.FastJsonUtils;
import jbm.framework.boot.autoconfigure.mqtt.AbstractMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.IMqttMessageListener;
import jbm.framework.boot.autoconfigure.mqtt.MqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author wesley
 */
@Slf4j
public class SimpleMqttClient {


    private final MqttProperties mqttProperties;

    private final Mqtt5AsyncClient mqttClient;

    public SimpleMqttClient(Mqtt5AsyncClient mqttClient, MqttProperties mqttProperties) {
        this.mqttClient = mqttClient;
        this.mqttProperties = mqttProperties;
    }

    public MqttClient getClient() {
        return this.mqttClient;
    }


    public void subscribe(String topicFilter, Consumer<Mqtt5Publish> messageListener) {
        // 订阅主题并设置消息监听器
        mqttClient.subscribeWith()
                .topicFilter(topicFilter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(messageListener)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Failed to subscribe to topic: " + throwable.getMessage());
                        return;
                    }
                    System.out.println(mqttClient.getConfig().getClientIdentifier()+"Subscribed to topic: " + topicFilter);
                });
    }

    public void subscribeWithResponse(String topicFilter, IMqttMessageListener mqttRequestListener) {
        this.subscribe(topicFilter, mqttRequestListener);
    }

    public String sendAndResponse(String requsetTopic, String responseTopic, Object requestMessage) {
        // 使用CountDownLatch来同步等待响应
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> response = new AtomicReference<>();
        this.subscribeWithResponse(responseTopic, new AbstractMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = message.getPayloadStr();
                if (topic.equals(responseTopic)) {
                    // 这里可以添加处理响应的逻辑
                    // 通知主线程可以结束了
                    latch.countDown();
                    response.set(payload);
                    unsubscribe(responseTopic); // 取消订阅响应主题
                } else {
                    log.error("not my response:{}", payload);
                }
            }
        });
        // 构建请求消息
        String requestPayload = null;
        if (requestMessage instanceof String) {
            requestPayload = (String) requestMessage;
        } else {
            requestPayload = JSON.toJSONString(requestMessage);
        }
        if (StrUtil.isBlank(requestPayload)) {
            throw new IllegalArgumentException("requestMessage must not be null");
        }
        MqttMessage mqttMessage = new MqttMessage(requestPayload.getBytes());
        mqttMessage.setQos(1);
        // 发送请求消息
        this.publish(requsetTopic, mqttMessage);
        // 在这里等待响应
        try {
            latch.await(30, TimeUnit.SECONDS);
            return response.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void publish(String topic, MqttMessage message) {
        this.mqttClient.publishWith().topic(topic).payload(message.getPayload()).qos(MqttQos.AT_LEAST_ONCE).send();
    }

    public void publishObject(String topic, Object message) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(JSON.toJSONBytes(message, FastJsonUtils.defaultWebConfig()));
        mqttMessage.setQos(1);
        this.publish(topic, mqttMessage);
    }


//    public boolean isConnected() {
//        return this.mqttClient.connectWith().
//    }
//
//    public void connect() {
//        if (!this.mqttClient.isConnected()) {
//            this.mqttClient.connect(mqttConnectProperties.toMqttConnectOptions());
//        }
//    }

    public void unsubscribe(String topicFilter) {
        this.mqttClient.unsubscribeWith().topicFilter(topicFilter).send();
    }

}
