package com.jbm.sample.mysql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.jbm")
public class MySqlAppStart {
	public static void main(String[] args) {
		SpringApplication.run(MySqlAppStart.class, args);
	}

}
