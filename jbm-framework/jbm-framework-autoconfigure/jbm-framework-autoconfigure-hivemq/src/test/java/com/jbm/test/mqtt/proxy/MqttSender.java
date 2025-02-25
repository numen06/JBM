package com.jbm.test.mqtt.proxy;

import com.jbm.framework.metadata.usage.bean.IBaseForm;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttSend;

import java.util.Map;

@MqttMapper
public interface MqttSender {

    @MqttSend( toTopic = "/test/from")
    void testfrom(Map<String, Object> msg);
}
