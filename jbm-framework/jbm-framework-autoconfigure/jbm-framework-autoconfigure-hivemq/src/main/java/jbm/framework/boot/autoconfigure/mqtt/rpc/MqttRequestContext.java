package jbm.framework.boot.autoconfigure.mqtt.rpc;

import jbm.framework.boot.autoconfigure.mqtt.hivemq.MqttMessage;

import java.util.concurrent.TimeUnit;

/**
 * 封装一次 MQTT 请求所需的上下文信息。
 */
public class MqttRequestContext {

    private final String requestTopic;
    private final String responseTopic;
    private final MqttMessage mqttMessage;
    private final String requestId;
    private final long timeout;
    private final TimeUnit timeUnit;

    public MqttRequestContext(String requestTopic,
                              String responseTopic,
                              MqttMessage mqttMessage,
                              String requestId,
                              long timeout,
                              TimeUnit timeUnit) {
        this.requestTopic = requestTopic;
        this.responseTopic = responseTopic;
        this.mqttMessage = mqttMessage;
        this.requestId = requestId;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public String getRequestTopic() {
        return requestTopic;
    }

    public String getResponseTopic() {
        return responseTopic;
    }

    public MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getTimeoutMillis() {
        return timeUnit.toMillis(timeout);
    }
}

