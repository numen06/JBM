package com.jbm.test.mqtt.call;

import com.alibaba.fastjson.JSONObject;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MqttCallClient(clientId = "mqttCallBean", requestTopic = "+/request", responseTopic = "test/response")
public class MqttCallServiceImpl implements MqttCallService {

    @MqttCallEvent("test.call")
    @Override
    public JSONObject call(@MqttBody String message) {
        JSONObject jsonObject = JSONObject.parseObject(message);
        jsonObject.put("result", "success");
//        String msg = StrUtil.format("Hello,{}", message);
        log.info("mqtt call: {}", message);
        return jsonObject;
    }

}
