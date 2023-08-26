package com.jbm.cluster.bigscreen;

import com.jbm.cluster.api.constants.BaseConstants;
import com.jbm.cluster.api.model.entitys.bigscreen.BigscreenView;
import com.jbm.cluster.bigscreen.common.BigscreenConstants;
import com.jbm.cluster.bigscreen.mapper.BigscreenViewMapper;
import com.jbm.cluster.common.health.DependsOnHealth;
import com.jbm.framework.masterdata.code.EnableCodeAutoGeneate;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@Slf4j
@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
@ComponentScans(value = {@ComponentScan(basePackageClasses = JbmBigscreenApplication.class)})
@EntityScan(basePackages = {"com.jbm.cluster.api.model.entitys.bigscreen"})
@MapperScan(basePackageClasses = BigscreenViewMapper.class)
@EnableCodeAutoGeneate(entityPackageClasses = {BigscreenView.class}, targetPackage = "com.jbm.cluster.bigscreen")
@Configuration
public class JbmBigscreenApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(JbmBigscreenApplication.class, args);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File baseDir = new File(BigscreenConstants.ZIP_DIR);
        try {
            log.info("静态地址是:{}", baseDir.getCanonicalPath());
            registry.addResourceHandler("/views/**").addResourceLocations("file:views/");
        } catch (IOException e) {
            log.error("获取静态地址错误");
        }
//        registry.addResourceHandler("/static/**").addResourceLocations("file:" + baseDir.getCanonicalPath(), "classpath:/static/");
    }

    /**
     * 启动监测
     * @param discoveryClient
     * @param applicationContext
     * @return
     */
    @Bean
    public DependsOnHealth dependsOnEndpoint(@Autowired DiscoveryClient discoveryClient, @Autowired ApplicationContext applicationContext) {
        return new DependsOnHealth(discoveryClient,applicationContext, BaseConstants.DOC_SERVER);
    }

}

