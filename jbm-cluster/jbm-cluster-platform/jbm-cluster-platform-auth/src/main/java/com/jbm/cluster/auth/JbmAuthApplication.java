package com.jbm.cluster.auth;

import com.jbm.cluster.auth.service.SaOAuth2TemplateImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 认证授权中心
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableFeignClients(basePackages = "com.jbm.cluster")
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class JbmAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(JbmAuthApplication.class, args);
    }

    @Bean
    @Primary
    public SaOAuth2TemplateImpl saOAuth2Template(){
        return new SaOAuth2TemplateImpl();
    }
}
