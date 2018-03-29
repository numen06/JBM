package com.td.sample.influx.app;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.td.micro.boot.MicroBootApplication;

@MicroBootApplication
@EnableScheduling
public class SampleInfluxAppStart {

	public static void main(String[] args) {
		SpringApplication.run(SampleInfluxAppStart.class, args);
	}
}
