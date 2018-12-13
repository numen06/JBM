package com.jbm.framework.cloud.config.test;

import com.jbm.framework.cloud.config.JbmConfigApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableConfigServer
@SpringBootApplication
@ComponentScan("com.jbm")
public class ConfigApplication   {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }

}