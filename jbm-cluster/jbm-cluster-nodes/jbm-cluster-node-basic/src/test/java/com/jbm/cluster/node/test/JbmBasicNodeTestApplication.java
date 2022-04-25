package com.jbm.cluster.node.test;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@EnableCaching
@SpringBootApplication
public class JbmBasicNodeTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmBasicNodeTestApplication.class, args);
    }

}

