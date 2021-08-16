package com.jbm.cluster.doc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * 平台文档服务
 * 提供文档的增删下载
 *
 * @author wesley.zhang
 */
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
public class JbmDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmDocApplication.class, args);
    }


}
