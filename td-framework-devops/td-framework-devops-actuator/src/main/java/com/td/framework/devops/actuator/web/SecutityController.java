package com.td.framework.devops.actuator.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/")
@Controller
public class SecutityController {
	@RequestMapping(value = "/login", headers = "Accept=text/html")
	public String login() {
		return "/login";
	}

//	@RequestMapping(value = "/login", method = RequestMethod.POST)
//	public String loginRequest() {
//		return "/login";
//	}

}
