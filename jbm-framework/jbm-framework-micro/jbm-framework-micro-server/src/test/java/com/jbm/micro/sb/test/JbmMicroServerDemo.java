package com.jbm.micro.sb.test;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import com.jbm.micro.sb.test.service.SpringBootService;

@EnableAutoConfiguration // 启用自动配置
@ComponentScan // 组件扫描
public class JbmMicroServerDemo {
	@Autowired
	private SpringBootService springBootService;

	@PostConstruct
	private void init() {
		System.err.println(springBootService.toString());
	}

	public static void main(String[] args) {
		// 启动Spring Boot项目的唯一入口
		SpringApplication.run(JbmMicroServerDemo.class, args);
	}
}
