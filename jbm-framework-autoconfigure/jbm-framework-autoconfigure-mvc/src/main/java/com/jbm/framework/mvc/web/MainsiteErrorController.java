package com.jbm.framework.mvc.web;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created with IntelliJ IDEA. User: Joe Xie Date: 2016/3/15 Time: 11:48
 */
@Controller
public class MainsiteErrorController implements ErrorController {

	private static final String ERROR_PATH = "/error";

	@RequestMapping(value = ERROR_PATH)
	public String handleError() {
		return "/error";
	}

	@Override
	public String getErrorPath() {
		return ERROR_PATH;
	}
}
