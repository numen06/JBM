package com.jbm.micro.sb.test;

import com.jbm.micro.sb.test.service.SpringBootService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;

@EnableAutoConfiguration // 启用自动配置
@ComponentScan // 组件扫描
public class SpringBootDemo {
    @Autowired
    private SpringBootService springBootService;

    public static void main(String[] args) {
        // 启动Spring Boot项目的唯一入口
        SpringApplication.run(SpringBootDemo.class, args);
    }

    @PostConstruct
    private void init() {
        System.err.println(springBootService.toString());
    }
}
