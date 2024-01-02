package com.jbm.test.mqtt;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.test.mqtt.proxy.MqttSender;
import com.jbm.test.mqtt.proxy.MqttSender2;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
import jbm.framework.boot.autoconfigure.mqtt.registrar.EnableMqttMapperScan;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@EnableMqttMapperScan("com.jbm.test.mqtt")
@SpringBootTest(
        classes = {MqttAutoConfiguration.class, MqttExecuteImpl.class})
@Slf4j
public class MqttProxy {
    @Autowired
    private MqttProxyFactory mqttProxyFactory;

    @Autowired
    private MqttSender mqttSender;
    @Autowired
    private MqttSender2 mqttSender2;
    @BeforeEach
    public void testClient() throws Exception {
    }

    @Test
    public void test() {
        mqttSender.testfrom(JSON.toJSONString(MapUtil.of("ttest","1212")));
        mqttSender2.toTwo("test2");
        ThreadUtil.waitForDie();
    }

}
