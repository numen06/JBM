package com.jbm.sample.app;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jbm.micro.boot.MicroBootApplication;

@MicroBootApplication
// @IntegrationComponentScan
@EnableScheduling
public class SampleInfluxAppStart {

	public static void main(String[] args) {
		SpringApplication.run(SampleInfluxAppStart.class, args);
	}
}
