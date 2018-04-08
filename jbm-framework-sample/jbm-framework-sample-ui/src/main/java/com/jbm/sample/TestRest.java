package com.jbm.sample;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TestRest {

	@RequestMapping("/test/hello")
	public String hello() {
		return "hello";
	}
}
