package com.td.test.dubbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.td")
public class DubboApp {
	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(DubboApp.class, args);
		Thread.sleep(10000);
	}
}
