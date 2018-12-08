package com.jbm.framework.mvc.web;

import java.net.URL;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jbm.util.StringUtils;

public class SpringBootWebTest {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	@LocalServerPort
	protected int port;
	protected URL base;
	@Autowired
	protected TestRestTemplate restTemplate;

	@Before
	public void setUp() throws Exception {
		String url = String.format("http://localhost:%d/", port);
		logger.info("port is : [{}]", port);
		this.base = new URL(url);
	}

	public String buildUrl(String method) {
		String url = this.base.toString() + "/" + method;
		logger.info("url is : [{}]", url);
		return url;
	}

	public JSONObject entityToModel(Object... objs) {
		JSONObject json = new JSONObject();
		for (Object obj : objs) {
			String className = StringUtils.uncapitalize(obj.getClass().getSimpleName());
			json.put(className, obj);
		}
		return json;
	}

	public ResponseEntity<String> request(String method, Object... urlVariables) {
		ResponseEntity<String> response = this.restTemplate.getForEntity(this.buildUrl(method), String.class,
				urlVariables);
		return response;
	}

	public ResponseEntity<String> requestJsonData(String method, Object obj) {
		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
		HttpEntity<String> formEntity = new HttpEntity<>(JSON.toJSONString(obj), headers);
		ResponseEntity<String> response = this.restTemplate.postForEntity(this.buildUrl(method), formEntity,
				String.class);
		return response;
	}

	public Object requestJson(String method, Object urlVariables) {
		String body = this.requestJsonData(method, urlVariables).getBody();
		return JSON.parse(body);
	}

	public JSONObject requestJsonObject(String method, Object urlVariables) {
		String body = this.requestJsonData(method, urlVariables).getBody();
		return JSON.parseObject(body);
	}

	public JSONArray requestJsonArray(String method, Object urlVariables) {
		String body = this.requestJsonData(method, urlVariables).getBody();
		return JSON.parseArray(body);
	}
}
