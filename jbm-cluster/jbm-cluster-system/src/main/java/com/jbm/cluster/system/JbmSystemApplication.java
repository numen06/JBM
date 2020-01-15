package com.jbm.cluster.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * 平台基础服务
 * 提供系统用户、权限分配、资源、客户端管理
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan(basePackages = "com.jbm.cluster.server.mapper")
public class JbmSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmSystemApplication.class, args);
    }
}
