package com.jbm.framework.devops.client.application;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.jbm.framework.devops.client.bean.ConfigOption;

@SpringBootApplication
@ComponentScan("com.td")
public class DevOpsClientApplication {
	private final static Logger logger = LoggerFactory.getLogger(DevOpsClientApplication.class);

	public static void main(String[] args) {
		try {
			CmdLineParser parser = new CmdLineParser(ConfigOption.getInstance());
			parser.parseArgument(args);
			parser.printUsage(System.out);
		} catch (CmdLineException e) {
			logger.error("参数错误", e);
			return;
		}
		SpringApplication.run(DevOpsClientApplication.class, args);
	}
}
