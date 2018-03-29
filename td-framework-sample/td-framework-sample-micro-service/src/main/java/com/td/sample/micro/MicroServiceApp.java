package com.td.sample.micro;

import org.springframework.boot.SpringApplication;

import com.td.micro.boot.MicroBootApplication;

@MicroBootApplication
public class MicroServiceApp {

	public static void main(String[] args) {
		SpringApplication.run(MicroServiceApp.class, args);
	}

}
