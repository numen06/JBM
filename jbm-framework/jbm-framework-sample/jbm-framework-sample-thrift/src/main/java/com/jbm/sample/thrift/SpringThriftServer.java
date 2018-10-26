package com.jbm.sample.thrift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringThriftServer {

	public static void main(String[] args) {
		SpringApplication.run(SpringThriftServer.class, args);
	}
}
