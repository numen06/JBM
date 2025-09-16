package jbm.framework.boot.autoconfigure.base.prometheus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wesley
 */
@Data
@ConfigurationProperties(prefix = "management.metrics.export.prometheus")
public class MetricsProperties {

    /**
     * 采集频率
     */
    private Integer rate;

}
