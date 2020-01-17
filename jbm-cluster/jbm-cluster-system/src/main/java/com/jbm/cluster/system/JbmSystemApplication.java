package com.jbm.cluster.system;

import com.jbm.cluster.system.mapper.BaseMenuMapper;
import jbm.framework.cloud.node.annotation.EnableJbmNodeServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

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
@MapperScan(basePackageClasses = BaseMenuMapper.class)
public class JbmSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmSystemApplication.class, args);
    }
}

