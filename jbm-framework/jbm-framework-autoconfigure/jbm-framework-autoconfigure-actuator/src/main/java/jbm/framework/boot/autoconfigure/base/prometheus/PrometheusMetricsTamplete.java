package jbm.framework.boot.autoconfigure.base.prometheus;

import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.List;
import java.util.Map;

/**
 * @author wesley
 */
public class PrometheusMetricsTamplete {

    private final PrometheusMeterRegistry prometheusRegistry;

    public PrometheusMetricsTamplete(PrometheusMeterRegistry prometheusRegistry) {
        this.prometheusRegistry = prometheusRegistry;
    }
    /**
     * 模拟 Prometheus 的 scrape 行为
     */
    private String scrape() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(prometheusRegistry.scrape());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write metrics", e);
        }
        //去掉带注解的行，减少发送体积
        return sb.toString().replaceAll("(#[^\\n]*\n)", "");
    }

    public String getMetricsAsText() {
        return this.scrape();
    }

    public List<Map<String, Object>> getMetrics() {
        String metrics = prometheusRegistry.scrape();
        return PrometheusMetricsParser.parseToMap(metrics);
    }

    public void printKeyMetrics() {
        List<Map<String, Object>> metrics = getMetrics();
        PrometheusMetricsPrinter.printKeyMetrics(metrics);
    }

    public void printKeyMetrics(List<Map<String, Object>> metrics){
        PrometheusMetricsPrinter.printKeyMetrics(metrics);
    }
}
