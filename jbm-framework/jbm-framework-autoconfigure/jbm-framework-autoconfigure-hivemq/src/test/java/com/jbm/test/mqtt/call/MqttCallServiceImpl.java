package com.jbm.test.mqtt.call;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @MqttCallEvent("test.call.array")
    @Override
    public JSONArray callArray(@MqttBody JSONArray message) {
        return message;
    }

    @MqttCallEvent("test.call.map")
    @Override
    public Map<String, String> callMap(@MqttBody Map<String, String> message) {
        return message;
    }

    @MqttCallEvent("test.call.list")
    @Override
    public List<String> callList(@MqttBody List<String> message) {
        return message;
    }

}
