package com.jbm.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoggerStart {

	public static void main(String[] args) {
		System.setProperty("java.util.logging.FileHandler.limit", "1");
		SpringApplication.run(LoggerStart.class, args);
	}
}
