package com.jbm.framework.devops.actuator.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.jbm.framework.devops.actuator.common.ActuatorConfig;

/**
 * @author wesley.zhang
 * @EnableAdminServer
 */
@SpringBootApplication
@ComponentScan("com.td")
@EntityScan("com.jbm.framework.devops.actuator.masterdata")
@EnableJpaRepositories("com.jbm.framework.devops.actuator.masterdata")
@EnableTransactionManagement
@EnableConfigurationProperties(ActuatorConfig.class)
@EnableScheduling
public class DevOpsActuatorApplication {
	// private final static Logger logger =
	// LoggerFactory.getLogger(DevOpsActuatorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DevOpsActuatorApplication.class, args);
	}

}
