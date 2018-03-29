package com.td.sample.leveldb.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.td")
public class LeveldbAppStart {

	public static void main(String[] args) {
		SpringApplication.run(LeveldbAppStart.class, args);
	}
}
