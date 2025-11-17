package com.jbm.cluster.logs;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class JbmLogsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmLogsApplication.class, args);
    }

}

