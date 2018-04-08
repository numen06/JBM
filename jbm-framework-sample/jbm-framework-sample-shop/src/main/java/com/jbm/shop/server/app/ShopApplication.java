package com.jbm.shop.server.app;

import org.springframework.boot.SpringApplication;

import com.jbm.micro.boot.web.MicroWebApplication;

@MicroWebApplication
public class ShopApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
	}
}
