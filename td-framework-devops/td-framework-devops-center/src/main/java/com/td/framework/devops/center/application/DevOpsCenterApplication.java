package com.td.framework.devops.center.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author wesley.zhang
 * @EnableAdminServer
 */
@SpringBootApplication
@ComponentScan("com.td")
@EnableTransactionManagement
@EnableScheduling
public class DevOpsCenterApplication {
	// private final static Logger logger =
	// LoggerFactory.getLogger(DevOpsActuatorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DevOpsCenterApplication.class, args);
	}

}
