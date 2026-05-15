package jbm.framework.boot.autoconfigure.base.prometheus;


import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wesley
 */
@Slf4j
@Configuration
@ConditionalOnClass(PrometheusMeterRegistry.class)
@ConditionalOnBean(PrometheusMeterRegistry.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class MetricsConfiguration {

    private final PrometheusMeterRegistry prometheusRegistry;

    private final MetricsProperties metricsProperties;

    public MetricsConfiguration(PrometheusMeterRegistry prometheusRegistry, MetricsProperties metricsProperties) {
        this.prometheusRegistry = prometheusRegistry;
        this.metricsProperties = metricsProperties;
    }

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
