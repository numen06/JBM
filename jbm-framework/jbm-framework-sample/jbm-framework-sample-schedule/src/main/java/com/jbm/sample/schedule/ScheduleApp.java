package com.jbm.sample.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan("com.jbm")
public class ScheduleApp {
	public static void main(String[] args) {
		SpringApplication.run(ScheduleApp.class, args);
	}
}
