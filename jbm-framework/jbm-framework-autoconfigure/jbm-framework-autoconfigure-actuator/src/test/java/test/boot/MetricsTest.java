package test.boot;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import jbm.framework.boot.autoconfigure.base.prometheus.MetricsConfiguration;
import jbm.framework.boot.autoconfigure.base.prometheus.MetricsSchedule;
import jbm.framework.boot.autoconfigure.base.prometheus.event.MetricsEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = { MetricsConfiguration.class,MetricsTest.TestEventListenerConfig.class})
public class MetricsTest {

    @Autowired
    private TestEventListener testEventListener;

    @Autowired
    private Environment env;

    @Test
    public void test() throws Exception {
        log.info("当前激活环境:{}", env.getActiveProfiles());
        log.info("当前环境变量:{}", env.getProperty("management.endpoints.web.exposure.include"));
        ThreadUtil.safeSleep(20*1000);
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
        public void print(MetricsEvent metricsEvent){
            Console.log("监听到数据");
            Console.log(metricsEvent.getMetrics());
        }
    }
}
