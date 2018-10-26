package com.jbm.sample.leveldb.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.jbm")
public class LeveldbAppStart {

	public static void main(String[] args) {
		SpringApplication.run(LeveldbAppStart.class, args);
	}
}
