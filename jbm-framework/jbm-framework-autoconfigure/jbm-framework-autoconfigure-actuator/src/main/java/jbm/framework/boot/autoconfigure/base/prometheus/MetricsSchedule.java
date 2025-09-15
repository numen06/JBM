package jbm.framework.boot.autoconfigure.base.prometheus;

import com.google.common.util.concurrent.AbstractScheduledService;
import jbm.framework.boot.autoconfigure.base.prometheus.event.MetricsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
@Slf4j
public class MetricsSchedule extends AbstractScheduledService implements InitializingBean {

    private final Executor executor = Executors.newScheduledThreadPool(2);


    private final ApplicationEventPublisher applicationEventPublisher;
    private final PrometheusMetricsTamplete prometheusMetricsTamplete;
    private final MetricsProperties metricsProperties;
//    private final String dynamicCron;

    public MetricsSchedule(ApplicationEventPublisher applicationEventPublisher, PrometheusMetricsTamplete prometheusMetricsTamplete,  MetricsProperties metricsProperties) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.prometheusMetricsTamplete = prometheusMetricsTamplete;
//        this.dynamicCron = dynamicCron;
        this.metricsProperties = metricsProperties;

    }

    // 你要执行的任务：发送数据
    private void sendData() {
        List<Map<String, Object>> metrics = prometheusMetricsTamplete.getMetrics();
        MetricsEvent metricsEvent = new MetricsEvent(metrics);
        applicationEventPublisher.publishEvent(metricsEvent);
    }

    @Override
    protected void runOneIteration() {
        try {
            this.sendData();
        } catch (Exception e) {
            log.error("发送监控数据错误", e);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, metricsProperties.getRate(), TimeUnit.SECONDS);
    }

    @Override
    public void afterPropertiesSet()  {
        if (metricsProperties.getRate() != null){
            this.startAsync();
        }
    }
}
