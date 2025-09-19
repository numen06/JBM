package jbm.framework.boot.autoconfigure.base.prometheus;


import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;



/**
 * @author wesley
 */
@Slf4j
@EnableConfigurationProperties(MetricsProperties.class)
public class MetricsConfiguration {

    @Autowired
    private PrometheusMeterRegistry prometheusRegistry;

    @Autowired
    private MetricsProperties metricsProperties;

    @Bean
    public PrometheusMetricsTamplete getEmqxApiClientService() {
        return new PrometheusMetricsTamplete(prometheusRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "management.metrics.export.prometheus", name = "rate")
    public MetricsSchedule getMetricsSchedule(ApplicationEventPublisher applicationEventPublisher, PrometheusMetricsTamplete prometheusMetricsTamplete) {
        return new MetricsSchedule(applicationEventPublisher, prometheusMetricsTamplete, metricsProperties);
    }

}
