package com.jbm.test.mqtt.proxy.impl;

import com.jbm.test.mqtt.proxy.MqttSender;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;

@MqttMapper
public class MqttSenderImpl implements MqttSender {

    @MqttSend(toTopic = "/test/to")
    public void to(String msg) {

    }
}
