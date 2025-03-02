package com.jbm.test.mqtt.call;

import cn.hutool.core.util.StrUtil;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallClient;
import jbm.framework.boot.autoconfigure.mqtt.annotation.call.MqttCallEvent;

@MqttCallClient(clientId = "mqttCallBean", requestTopic = "+/request", responseTopic = "test/response")
public class MqttCallServiceImpl implements MqttCallService {

    @MqttCallEvent("test.call")
    @Override
    public String call(@MqttBody String message) {
        return StrUtil.format("Hello,{}", message);
    }

}
