package com.jbm.test.mqtt.call;

import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;

@MqttCallClient(requestTopic = "test/request", responseTopic = "test/response")
public interface MqttCallService {

    @MqttCallEvent("test.call")
    String call( String message);

}
