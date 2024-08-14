package com.jbm.cluster.common.basic.configuration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fanscat
 * @createTime 2024/6/6 16:31
 */
@Data
@ConfigurationProperties(prefix = "jbm.tenant")
public class TenantProperties {
    private Boolean enabled = true;
    private String initializeFile;
}
