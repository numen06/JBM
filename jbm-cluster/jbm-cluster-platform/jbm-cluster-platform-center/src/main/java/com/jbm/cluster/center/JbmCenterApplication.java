package com.jbm.cluster.center;

import com.jbm.cluster.center.mapper.BaseMenuMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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
@EntityScan(basePackages = {"com.jbm.cluster.api.model"})
@MapperScan(basePackageClasses = BaseMenuMapper.class)
public class JbmCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmCenterApplication.class, args);
    }


}

