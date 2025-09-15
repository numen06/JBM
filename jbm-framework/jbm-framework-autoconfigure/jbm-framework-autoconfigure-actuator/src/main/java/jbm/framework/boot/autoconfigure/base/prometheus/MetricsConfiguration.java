package jbm.framework.boot.autoconfigure.base.prometheus;


import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author wesley
 */
@Slf4j
@Configuration
public class MetricsConfiguration {

    @Resource
    private PrometheusMeterRegistry prometheusRegistry;

    @Bean
    public PrometheusMetricsTamplete getEmqxApiClientService() {
        return new PrometheusMetricsTamplete(prometheusRegistry);
    }


}
