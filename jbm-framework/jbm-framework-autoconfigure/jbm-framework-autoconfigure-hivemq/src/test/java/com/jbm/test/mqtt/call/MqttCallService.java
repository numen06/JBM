package com.jbm.test.mqtt.call;

import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttParam;

import java.util.Date;

@MqttCallClient(requestTopic = "test/request", responseTopic = "test/response")
public interface MqttCallService {

    @MqttCallEvent("test.call")
    String call(@MqttBody String message);

    @MqttCallEvent("test.call")
    String call(@MqttParam("time") Date time);
}
