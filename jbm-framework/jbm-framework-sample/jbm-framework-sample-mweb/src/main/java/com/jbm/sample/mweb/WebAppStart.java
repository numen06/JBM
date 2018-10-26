package com.jbm.sample.mweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author wesley
 *
 */
@SpringBootApplication
@ComponentScan("com.jbm")
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
