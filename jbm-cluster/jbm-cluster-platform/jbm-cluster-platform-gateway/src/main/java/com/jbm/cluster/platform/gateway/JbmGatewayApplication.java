package com.jbm.cluster.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * 网关启动程序
 *
 * @author wesley.zhang
 */
@EnableWebFlux
@SpringBootApplication
public class JbmGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmGatewayApplication.class, args);
    }
}
