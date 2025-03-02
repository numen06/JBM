package com.jbm.test.mqtt.call;

import com.alibaba.fastjson.JSONObject;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;

@MqttCallClient(requestTopic = "test/request", responseTopic = "test/response")
public interface MqttCallService {

    @MqttCallEvent("test.call")
    JSONObject call(@MqttBody String message);

}
