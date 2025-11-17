package jbm.framework.boot.autoconfigure.base.prometheus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

/**
 * @author wesley
 */
public class MetricsEvent extends ApplicationEvent {

    @Getter
    private final List<Map<String, Object>> metrics;
    public MetricsEvent(List<Map<String, Object>> metrics) {
        super(metrics);
        this.metrics = metrics;
    }



}
