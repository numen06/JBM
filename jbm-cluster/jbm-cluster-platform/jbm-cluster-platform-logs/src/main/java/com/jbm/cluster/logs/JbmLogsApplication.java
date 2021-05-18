package com.jbm.cluster.logs;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "com.jbm.cluster.logs.repository")
@EnableDubbo(scanBasePackages = "com.jbm.cluster.api")
public class JbmLogsApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmLogsApplication.class, args);
    }

}

