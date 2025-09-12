package com.jbm.test;

import jbm.framework.boot.autoconfigure.emqx.EmqxApiListener;
import jbm.framework.boot.autoconfigure.emqx.configuration.EmqxConfiguration;
import jbm.framework.boot.autoconfigure.emqx.event.EmqxClientEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {EmqxConfiguration.class, EmqxApiListenerTest.TestEventListenerConfig.class})
public class EmqxApiListenerTest {

    @Resource
    private EmqxApiListener emqxApiListener;

    @Autowired
    private TestEventListener testEventListener;

    @Test
    public void test() throws Exception {
        Thread.sleep(1000000);
    }

    @TestConfiguration
    public static class TestEventListenerConfig {

        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    public static class TestEventListener {
        @EventListener
        public void handleDeviceStatusChange(EmqxClientEvent event) {
            log.info("✅ [测试监听器] 监听到设备[{}]状态事件: {},", event.getEmqxClient().getClientId(), event.getStatus());
        }
    }
}
