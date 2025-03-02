package com.jbm.test.mqtt.call;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;

import java.util.List;
import java.util.Map;

@MqttCallClient(requestTopic = "test/request", responseTopic = "test/response")
public interface MqttCallService {

    @MqttCallEvent("test.call")
    JSONObject call(@MqttBody String message);


    @MqttCallEvent("test.call.array")
    JSONArray callArray(@MqttBody JSONArray message);

    @MqttCallEvent("test.call.map")
    Map<String,String> callMap(@MqttBody Map<String,String> message);

    @MqttCallEvent("test.call.list")
    List<String> callList(@MqttBody List<String> message);
}
