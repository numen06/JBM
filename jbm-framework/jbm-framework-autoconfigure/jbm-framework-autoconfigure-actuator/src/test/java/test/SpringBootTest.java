package test;

import cn.hutool.core.lang.Console;
import jbm.framework.boot.autoconfigure.base.prometheus.PrometheusMetricsPrinter;
import jbm.framework.boot.autoconfigure.base.prometheus.PrometheusMetricsParser;
import jbm.framework.boot.autoconfigure.base.prometheus.PrometheusMetricsTamplete;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class SpringBootTest {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTest.class, args);
    }

    @Service
    public static class TestEventListener implements InitializingBean {
        @Resource
        private PrometheusMetricsTamplete prometheusMetricsTamplete;
        /**
         */
        @Override
        public void afterPropertiesSet() {
            log.info("初始化完成");
            String txt = prometheusMetricsTamplete.getMetricsAsText();
            Console.log(txt);
            List<Map<String, Object>> json = PrometheusMetricsParser.parseToMap(txt);
            PrometheusMetricsPrinter.printKeyMetrics(json);

        }
    }




}
