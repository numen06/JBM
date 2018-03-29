
package com.td.sample.wechat.service;

import javax.annotation.PostConstruct;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.sword.wechat4j.jsapi.JsApiManager;
import org.sword.wechat4j.jsapi.JsApiParam;

import java.io.*;

@RequestMapping(value = "/")
@Controller
public class MainController {
	private JsApiParam jsApiParam;

	@PostConstruct
	public void init() {
		jsApiParam = JsApiManager.signature("http://www.tdenergys.com/mwechat/views");

	}

	@RequestMapping(value = "/", headers = "Accept=text/html")
	public ModelAndView index(HttpServletRequest request, ModelAndView mav) {
		// 获取完整的URL地址
		mav.addObject("appId", jsApiParam.getAppid());
		mav.addObject("timestamp", jsApiParam.getTimeStamp());
		mav.addObject("nonceStr", jsApiParam.getNonceStr());
		mav.addObject("signature", jsApiParam.getSignature());
		mav.setViewName("/index");
		return mav;
	}



	@RequestMapping(value = "/views/{path}", headers = "Accept=text/html")
	public String views(@PathVariable String path) {
		return "/views/" + path;
	}

	@RequestMapping(value = "/views/{path}.txt")
	public String views2(@PathVariable String path) {
		return "/views/" + path;
	}


	@RequestMapping(value = "/views/{path}/{html}", headers = "Accept=text/html")
	public String html(@PathVariable String path, @PathVariable String html) {
		return "/views/" + path + "/" + html;
	}

}
