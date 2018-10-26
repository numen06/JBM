
package com.jbm.webjars;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = "/")
@Controller
public class MainController {

	@RequestMapping(value = "/", headers = "Accept=text/html")
	public String index() {
		return "/index";
	}

	@RequestMapping(value = "/{path}:^(?!index|error).*$", headers = "Accept=text/html")
	public String index(@PathVariable String path) {
		return "/" + path;
	}

	@RequestMapping(value = "/views/{path}", headers = "Accept=text/html")
	public String views(@PathVariable String path) {
		return "/views/" + path;
	}

	@RequestMapping(value = "/views/{path}/{html}", headers = "Accept=text/html")
	public String html(@PathVariable String path, @PathVariable String html) {
		return "/views/" + path + "/" + html;
	}

	@RequestMapping(value = "/views/partials/{path}", headers = "Accept=text/html")
	public String partials(@PathVariable String path) {
		return "/views/partials/" + path;
	}

}
