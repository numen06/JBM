package com.baidu.disconf.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EntityScan("com.baidu")
@EnableJpaRepositories("com.baidu")
@ComponentScan({ "com.baidu", "com.td" })
public class DisconfAppStart {
	public static void main(String[] args) {
		SpringApplication.run(DisconfAppStart.class, args);
	}
}
