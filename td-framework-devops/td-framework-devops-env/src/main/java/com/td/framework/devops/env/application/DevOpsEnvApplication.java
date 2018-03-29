package com.td.framework.devops.env.application;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.td.framework.devops.env.bean.ConfigOption;

@SpringBootApplication
@ComponentScan("com.td")
public class DevOpsEnvApplication {
	// private final static Logger logger =
	// LoggerFactory.getLogger(DevOpsEnvApplication.class);

	public static void main(String[] args) {
		try {
			CmdLineParser parser = new CmdLineParser(ConfigOption.getInstance());
			parser.parseArgument(args);
			parser.printUsage(System.out);
		} catch (CmdLineException e) {
			return;
		}
		SpringApplication.run(DevOpsEnvApplication.class, args);
	}
}
