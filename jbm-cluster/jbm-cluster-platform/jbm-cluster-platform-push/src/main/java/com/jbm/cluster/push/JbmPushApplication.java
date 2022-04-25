package com.jbm.cluster.push;

import com.jbm.cluster.api.event.UserLoginEvent;
import com.jbm.cluster.api.model.entitys.message.PushMessage;
import com.jbm.cluster.push.mapper.PushConfigInfoMapper;
import com.jbm.framework.masterdata.code.EnableCodeAutoGeneate;
import jbm.framework.boot.autoconfigure.eventbus.annotation.EnableClusterEventBus;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


/**
 * 平台文档服务
 * 提供文档的增删下载
 *
 * @author wesley.zhang
 */
@EnableClusterEventBus(basePackages = {"com.jbm.cluster.push"})
@EnableFeignClients("com.jbm.cluster.push.fegin")
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EntityScan(basePackageClasses = {PushMessage.class})
@EnableTransactionManagement
@MapperScan(basePackageClasses = {PushConfigInfoMapper.class})
@RemoteApplicationEventScan(basePackageClasses = UserLoginEvent.class)
@EnableCodeAutoGeneate(entityPackageClasses = {PushMessage.class}, targetPackage = "com.jbm.cluster.push")
public class JbmPushApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmPushApplication.class, args);
    }


}
