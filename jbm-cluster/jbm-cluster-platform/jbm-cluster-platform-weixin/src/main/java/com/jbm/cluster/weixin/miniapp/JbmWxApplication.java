package com.jbm.cluster.weixin.miniapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableTransactionManagement
public class JbmWxApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmWxApplication.class, args);
    }
}
