package com.jbm.cluster.common.core.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 资源扫描配置
 *
 * @author wesley.zhang
 * @date 2018/7/29
 */
@ConfigurationProperties(prefix = "jbm.scan")
public class JbmScanProperties {

    /**
     * 请求资源注册到API列表
     */
    private Boolean registerRequestMapping = false;

    public boolean isRegisterRequestMapping() {
        return registerRequestMapping;
    }

    public void setRegisterRequestMapping(boolean registerRequestMapping) {
        this.registerRequestMapping = registerRequestMapping;
    }


}
