package com.jbm.test.mqtt.proxy.impl;


import com.jbm.test.mqtt.proxy.MqttExecute;
import com.jbm.util.proxy.annotation.RequestBody;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttRequest;
import jbm.framework.boot.autoconfigure.mqtt.client.SimpleMqttClient;
import jbm.framework.boot.autoconfigure.mqtt.useage.MqttResponseBean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Service;

import java.util.List;

@MqttMapper(clientId = "test")
@Service
@Slf4j
public class MqttExecuteImpl implements MqttExecute {

    private SimpleMqttClient simpleMqttClient;

    @Getter
    private String from = "from";

    @Override
    @MqttRequest(fromTopic = "/test/${from}")
    public void from() {
        log.info("do it");
    }

    @MqttRequest(fromTopic = "/test/to")
    public void test( String msg) {
        log.info("我只是来打印的,{}", msg);
    }

    @Override
    @MqttRequest(fromTopic = "/test/from", toTopic = "/test/to")
    public String to(@RequestBody String msg) {
        log.info(msg);
        try {
            simpleMqttClient.publishObject("/test/to", "test");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return "我是测试";
    }

    @MqttRequest(fromTopic = "/test/from1", toTopic = "/test/to")
    public String to(@RequestBody List<String> msg) {
        try {
            simpleMqttClient.publishObject("/test/to", "test");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return "我是测试";
    }

    @MqttRequest(fromTopic = "/test/from2")
    public MqttResponseBean toMqttResponseBean(@RequestBody String msg) {
        return new MqttResponseBean("/test/from2/result", msg);
    }


}
