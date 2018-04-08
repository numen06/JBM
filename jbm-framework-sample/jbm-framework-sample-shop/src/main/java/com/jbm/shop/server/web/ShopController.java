package com.jbm.shop.server.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ShopController {

	@RequestMapping("/start")
	public Object start() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("status", "success");
		return map;
	}
}
