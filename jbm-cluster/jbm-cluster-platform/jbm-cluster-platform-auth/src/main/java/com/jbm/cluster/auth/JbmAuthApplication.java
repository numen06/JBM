package com.jbm.cluster.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证授权中心
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableFeignClients(basePackages = "com.jbm.cluster")
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class JbmAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmAuthApplication.class, args);
    }
}
