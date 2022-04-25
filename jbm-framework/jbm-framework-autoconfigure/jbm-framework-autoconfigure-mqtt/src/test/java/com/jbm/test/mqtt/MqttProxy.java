package com.jbm.test.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.jbm.test.mqtt.proxy.impl.MqttExecuteImpl;
import jbm.framework.boot.autoconfigure.mqtt.MqttAutoConfiguration;
import jbm.framework.boot.autoconfigure.mqtt.proxy.MqttProxyFactory;
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
@SpringBootTest(classes = {MqttAutoConfiguration.class, MqttExecuteImpl.class})
@Slf4j
public class MqttProxy {
    @Autowired
    private MqttProxyFactory mqttProxyFactory;

    @BeforeEach
    public void testClient() throws Exception {
    }

    @Test
    public void test() {
        ThreadUtil.waitForDie();
    }

}
