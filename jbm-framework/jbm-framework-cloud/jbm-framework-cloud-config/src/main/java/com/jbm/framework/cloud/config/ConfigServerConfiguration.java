package com.jbm.framework.cloud.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.config.server.EnableConfigServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author: create by wesley
 * @date:2018/12/16
 */
@EnableConfigServer
public class ConfigServerConfiguration implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(ConfigServerConfiguration.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("jbm config server start");
    }
}
