package com.td.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainColl {

	@RequestMapping("/")
	public String index() {
		return "index";
	}
	
	@RequestMapping("/chat")
	public String chat() {
		return "chat";
	}
	
	@RequestMapping("/webworker")
	public String webworker() {
		return "webworker";
	}
}
