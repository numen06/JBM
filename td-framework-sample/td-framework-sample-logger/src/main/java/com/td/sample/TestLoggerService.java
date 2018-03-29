package com.td.sample;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jodd.util.PropertiesUtil;

@Service
public class TestLoggerService {
	private static final Logger sl4jLogger = LoggerFactory.getLogger(TestLoggerService.class);

	@PostConstruct
	public void init() throws InterruptedException {
		Properties p = new Properties();
		p.putAll(System.getenv());
		try {
			PropertiesUtil.writeToFile(p, new File("env.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < 100; i++) {
			Thread.sleep(100);
			sl4jLogger.info("我就是一个测试", new RuntimeException("test"));
		}
	}
}
