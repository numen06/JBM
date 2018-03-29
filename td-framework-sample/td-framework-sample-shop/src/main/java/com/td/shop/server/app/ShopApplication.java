package com.td.shop.server.app;

import org.springframework.boot.SpringApplication;

import com.td.micro.boot.web.MicroWebApplication;

@MicroWebApplication
public class ShopApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
	}
}
