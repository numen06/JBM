package com.jbm.framework.cloud.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @author: create by wesley
 * @date:2018/12/16
 */
@EnableEurekaServer
public class RegistryServerConfiguration implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(RegistryServerConfiguration.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("jbm eureke registry server start");
    }
}
