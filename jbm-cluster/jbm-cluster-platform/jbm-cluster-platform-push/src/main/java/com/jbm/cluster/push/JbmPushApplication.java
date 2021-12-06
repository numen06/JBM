package com.jbm.cluster.push;

import com.jbm.cluster.api.TestRemoteApplicationEvent;
import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.framework.masterdata.code.EnableCodeAutoGeneate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableScheduling;
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
@EnableScheduling
@EnableKafkaStreams
@EntityScan(basePackageClasses = {PushMessage.class})
@EnableTransactionManagement
@RemoteApplicationEventScan(basePackageClasses = TestRemoteApplicationEvent.class)
@EnableCodeAutoGeneate(entityPackageClasses = {PushMessage.class}, targetPackage = "com.jbm.cluster.push")
public class JbmPushApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmPushApplication.class, args);
    }


}
