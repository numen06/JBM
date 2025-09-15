package test;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import jbm.framework.boot.autoconfigure.base.listener.PrometheusTextToMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class SpringBootTest {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTest.class, args);
    }

    @Service
    public class TestEventListener implements InitializingBean {

        /**
         */
        @Override
        public void afterPropertiesSet() {
            log.info("初始化完成");

            String txt = scrape();
            List<Map<String, Object>> json = PrometheusTextToMap.parseToMap(txt);
            for (Map<String, Object> metric : json){
                log.info("获取应用状态：{}", metric);
            }
        }
    }


    @Autowired
    private PrometheusMeterRegistry prometheusRegistry;

//    @Bean
//    public PrometheusMeterRegistry prometheusMeterRegistry() {
//        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//    }

    /**
     * 模拟 Prometheus 的 scrape 行为
     */
    private String scrape() {
        CollectorRegistry registry = prometheusRegistry.getPrometheusRegistry();
        StringWriter writer = new StringWriter();
        try {
            TextFormat.write004(writer, registry.metricFamilySamples());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write metrics", e);
        }
        return writer.toString();
    }


}
