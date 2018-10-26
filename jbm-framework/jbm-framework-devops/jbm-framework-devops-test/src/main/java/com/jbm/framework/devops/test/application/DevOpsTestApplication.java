package com.jbm.framework.devops.test.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.jbm")
public class DevOpsTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevOpsTestApplication.class, args);
	}
}
