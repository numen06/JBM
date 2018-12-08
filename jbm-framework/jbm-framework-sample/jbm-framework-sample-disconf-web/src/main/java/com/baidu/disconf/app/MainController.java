
package com.baidu.disconf.app;

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

	@RequestMapping(value = "/index", headers = "Accept=text/html")
	public String index2() {
		return "/index";
	}

	@RequestMapping(value = "/login", headers = "Accept=text/html")
	public String login() {
		return "/login";
	}

	@RequestMapping(value = "/main", headers = "Accept=text/html")
	public String main() {
		return "/main";
	}

	@RequestMapping(value = "/modifyFile", headers = "Accept=text/html")
	public String modifyFile() {
		return "/modifyFile";
	}

	@RequestMapping(value = "/modifyItem", headers = "Accept=text/html")
	public String modifyItem() {
		return "/modifyItem";
	}

	@RequestMapping(value = "/modifypassword", headers = "Accept=text/html")
	public String modifypassword() {
		return "/modifypassword";
	}

	@RequestMapping(value = "/newapp", headers = "Accept=text/html")
	public String newapp() {
		return "/newapp";
	}

	@RequestMapping(value = "/newconfig_file", headers = "Accept=text/html")
	public String newconfig_file() {
		return "/newconfig_file";
	}

	@RequestMapping(value = "/newconfig_item", headers = "Accept=text/html")
	public String newconfig_item() {
		return "/newconfig_item";
	}

	@RequestMapping(value = "/newconfig", headers = "Accept=text/html")
	public String newconfig() {
		return "/newconfig";
	}

	// @RequestMapping(value = "/{path}:^(?!index|error).*$", headers =
	// "Accept=text/html")
	// public String index(@PathVariable String path) {
	// return "/" + path;
	// }

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
