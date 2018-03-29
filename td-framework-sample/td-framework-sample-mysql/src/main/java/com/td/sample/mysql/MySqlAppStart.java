package com.td.sample.mysql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.td")
public class MySqlAppStart {
	public static void main(String[] args) {
		SpringApplication.run(MySqlAppStart.class, args);
	}

}
