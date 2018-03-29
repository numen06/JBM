package com.td.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@IntegrationComponentScan
@ComponentScan("com.td")
public class MqttAppStart {
	public static void main(String[] args) {
		SpringApplication.run(MqttAppStart.class, args);
	}
}
