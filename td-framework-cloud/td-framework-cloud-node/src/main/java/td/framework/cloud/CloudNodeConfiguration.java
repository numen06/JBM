package td.framework.cloud;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudNodeConfiguration {
	@Bean
	public OptionsCorsFilter optionsCorsFilter() {
		return new OptionsCorsFilter();
	}
}
