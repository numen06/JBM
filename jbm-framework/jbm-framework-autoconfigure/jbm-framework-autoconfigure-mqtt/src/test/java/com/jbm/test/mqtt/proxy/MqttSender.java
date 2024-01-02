package com.jbm.test.mqtt.proxy;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;

@MqttMapper
public interface MqttSender {

    @MqttSend( toTopic = "/test/from")
    void testfrom(String msg);
}
