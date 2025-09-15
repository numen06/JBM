package test;

import cn.hutool.http.HttpRequest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootConfiguration
@SpringBootTest(classes = {MetricsTest.TestEventListenerConfig.class})
public class MetricsTest {

    @Autowired
    private TestEventListener testEventListener;


    @Autowired
    private Environment env;

    @Test
    public void test() throws Exception {
        log.info("当前激活环境:{}", env.getActiveProfiles());
        log.info("当前环境变量:{}", env.getProperty("management.endpoints.web.exposure.include"));

    }

    @TestConfiguration
    public static class TestEventListenerConfig {

        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    public static class TestEventListener {
        public void handleDeviceStatusChange() {

        }
    }
}
