package com.td.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/oauth")
@Controller
public class OAuthController {

	@RequestMapping("/confirm_access")
	public String confirm_access() {
		return "authorize";
	}

}
