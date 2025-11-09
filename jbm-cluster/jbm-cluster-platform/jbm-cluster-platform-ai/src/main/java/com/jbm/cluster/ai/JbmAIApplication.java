package com.jbm.cluster.ai;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author wesley
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class JbmAIApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmAIApplication.class, args);
    }
}
