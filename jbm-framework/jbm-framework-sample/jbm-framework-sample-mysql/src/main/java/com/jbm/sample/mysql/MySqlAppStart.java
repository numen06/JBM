package com.jbm.sample.mysql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import com.jbm.micro.boot.MicroBootApplication;
import com.jbm.sample.entity.AggregateData;

@MicroBootApplication
@EntityScan(basePackageClasses = AggregateData.class)
public class MySqlAppStart {
	public static void main(String[] args) {
		SpringApplication.run(MySqlAppStart.class, args);
	}

}
