package com.jbm.cluster.auth;

import com.jbm.cluster.auth.config.ThirdPartyAuthProperties;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证授权中心（独立数据源，不依赖 center 完成登录）
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableFeignClients(basePackages = "com.jbm.cluster")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.jbm.cluster.auth", "com.jbm.cluster.common.mysql"})
@EntityScan(basePackages = {"com.jbm.cluster.api.entitys"})
@MapperScan(basePackageClasses = BaseMenuMapper.class)
@EnableConfigurationProperties({ThirdPartyAuthProperties.class})
public class JbmAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmAuthApplication.class, args);
    }

}
