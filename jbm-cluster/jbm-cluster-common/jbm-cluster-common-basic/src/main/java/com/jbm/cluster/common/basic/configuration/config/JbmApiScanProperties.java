package com.jbm.cluster.common.basic.configuration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 资源扫描配置
 *
 * @author wesley.zhang
 * @date 2018/7/29
 */
@Data
@ConfigurationProperties(prefix = "jbm.scan")
public class JbmApiScanProperties {

    /**
     * 请求资源注册到API列表
     */
    private Boolean registerRequestMapping = false;


}
