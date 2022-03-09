package jbm.framework.boot.autoconfigure.mqtt.useage;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class MqttRequsetBean {

    private String requestTopic;

    private Method method;

    private Object bean;

    private String responseTopic;
}
