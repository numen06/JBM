package com.jbm.framework.devops.test.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

@Service
public class NetworkTest {
	private final static Logger logger = LoggerFactory.getLogger(NetworkTest.class);

	// private RestTemplate restTemplate =
	// RestTemplateFactory.getInstance().createRestTemplate();

	@PostConstruct
	public void init() {
		html();
	}

	public void html() {
		HttpRequest httpRequest = HttpRequest.get("http://jodd.org");
		HttpResponse response = httpRequest.send();
		logger.info(response.bodyText());
	}
}
