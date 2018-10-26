package com.jbm.sample.micro;

import org.springframework.boot.SpringApplication;

import com.jbm.micro.boot.MicroBootApplication;

@MicroBootApplication
public class MicroServiceApp {

	public static void main(String[] args) {
		SpringApplication.run(MicroServiceApp.class, args);
	}

}
