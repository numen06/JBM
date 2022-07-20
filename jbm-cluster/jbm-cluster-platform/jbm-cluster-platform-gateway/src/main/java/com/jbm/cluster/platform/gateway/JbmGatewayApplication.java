package com.jbm.cluster.platform.gateway;

import jbm.framework.boot.autoconfigure.eventbus.annotation.EnableClusterEventBus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * 网关启动程序
 *
 * @author wesley.zhang
 */
@EnableWebFlux
@EnableFeignClients(basePackages = "com.jbm.cluster")
@EnableClusterEventBus(basePackages = "com.jbm.cluster")
@RemoteApplicationEventScan(basePackages = "com.jbm.cluster")
@SpringBootApplication
public class JbmGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmGatewayApplication.class, args);
    }
}
