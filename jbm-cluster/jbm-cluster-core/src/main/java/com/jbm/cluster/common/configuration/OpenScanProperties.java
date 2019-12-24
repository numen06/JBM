package com.jbm.cluster.common.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 资源扫描配置
 *
 * @author LYD
 * @date 2018/7/29
 */
@ConfigurationProperties(prefix = "opencloud.scan")
public class OpenScanProperties {

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
