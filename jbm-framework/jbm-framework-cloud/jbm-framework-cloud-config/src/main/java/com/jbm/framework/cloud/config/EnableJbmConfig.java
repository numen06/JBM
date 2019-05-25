package com.jbm.framework.cloud.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Import;

/**
 * @author: create by wesley
 * @date:2018/12/15
 */
@EnableConfigServer
@SpringBootApplication
@Import({ConfigServerConfiguration.class})
public @interface EnableJbmConfig {
}
