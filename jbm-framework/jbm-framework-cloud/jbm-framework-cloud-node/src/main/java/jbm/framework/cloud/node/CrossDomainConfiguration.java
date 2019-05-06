package jbm.framework.cloud.node;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrossDomainConfiguration {
	@Bean
	public OptionsCorsFilter optionsCorsFilter() {
		return new OptionsCorsFilter();
	}
}
