package com.jbm.test.mqtt.proxy;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;

@MqttMapper
public interface MqttSender2 {

    @MqttSend( toTopic = "/test/to2")
    void toTwo(String msg);
}
