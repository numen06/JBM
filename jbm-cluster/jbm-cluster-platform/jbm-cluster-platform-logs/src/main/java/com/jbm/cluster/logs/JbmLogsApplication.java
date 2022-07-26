package com.jbm.cluster.logs;

//import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@EnableCaching
@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.jbm.cluster.logs.repository")
public class JbmLogsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmLogsApplication.class, args);
    }

}

