package com.jbm.cluster.bigscreen;

import com.jbm.cluster.api.model.entity.bigscreen.BigscreenView;
import com.jbm.cluster.bigscreen.mapper.BigscreenViewMapper;
import com.jbm.framework.masterdata.code.EnableCodeAutoGeneate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

/**
 * 日志收集基础服务
 *
 * @author wesley.zhang
 */
@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
@ComponentScans(value = {@ComponentScan(basePackageClasses = JbmBigscreenApplication.class)})
@EntityScan(basePackages = {"com.jbm.cluster.api.model.entity.bigscreen"})
@MapperScan(basePackageClasses = BigscreenViewMapper.class)
@EnableCodeAutoGeneate(entityPackageClasses = {BigscreenView.class}, targetPackage = "com.jbm.cluster.bigscreen")
public class JbmBigscreenApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmBigscreenApplication.class, args);
    }

}

