package jbm.framework.cloud.service.rest;

import java.security.Principal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ServiceAController {

	@RequestMapping("/test")
	public Object test(Principal principal) {
		return principal;
	}

}
