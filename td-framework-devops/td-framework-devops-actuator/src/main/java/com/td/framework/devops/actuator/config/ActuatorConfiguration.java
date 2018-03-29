package com.td.framework.devops.actuator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.td.framework.devops.actuator.common.ActuatorConfig;
import com.td.framework.devops.actuator.service.ProcessService;
import com.td.framework.devops.actuator.service.impl.LinuxProcessServiceImpl;
import com.td.framework.devops.actuator.service.impl.WinProcessServiceImpl;

@Configuration
public class ActuatorConfiguration {

	private static Logger logger = LoggerFactory.getLogger(ActuatorConfiguration.class);

	@Bean
	public ProcessService processService(SigarService sigarService) {
		logger.info("sigarService:{}", sigarService);
		ProcessService processService = null;
		if (ActuatorConfig.isWindows()) {
			processService = new WinProcessServiceImpl(sigarService);
		} else {
			processService = new LinuxProcessServiceImpl(sigarService);
		}
		return processService;
	}

	@Bean
	public SigarService sigarService() {
		SigarService sigarService = new SigarService();
		return sigarService;
	}

}
