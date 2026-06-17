package com.jbm.cluster.common.basic.configuration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 系统调试模式配置（环境变量 JBM_DEBUG 映射为 jbm.debug）
 */
@Data
@ConfigurationProperties(prefix = "jbm")
public class JbmDebugProperties {

    /**
     * 是否通过环境变量/配置开启调试模式
     */
    private Boolean debug = false;
}
