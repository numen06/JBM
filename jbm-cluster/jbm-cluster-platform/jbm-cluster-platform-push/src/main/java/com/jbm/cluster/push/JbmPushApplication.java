package com.jbm.cluster.push;

import com.jbm.cluster.api.model.entity.message.PushMessage;
import com.jbm.framework.masterdata.code.EnableCodeAutoGeneate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;


/**
 * 平台文档服务
 * 提供文档的增删下载
 *
 * @author wesley.zhang
 */
@EnableFeignClients("com.jbm.cluster.push.fegin")
@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackageClasses = {PushMessage.class})
@EnableTransactionManagement
@EnableCodeAutoGeneate(entityPackageClasses = {PushMessage.class}, targetPackage = "com.jbm.cluster.push")
public class JbmPushApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmPushApplication.class, args);
    }


}
