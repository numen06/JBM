package com.td.sample.mweb;

import org.springframework.boot.SpringApplication;

import com.td.micro.boot.web.MicroWebApplication;

/**
 * @author wesley
 *
 */
@MicroWebApplication
public class WebAppStart {

	/**
	 * web启动
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(WebAppStart.class, args);
	}
}
