package td.framework.boot.autoconfigure.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 默认的缓存注入
 * 
 * @author wesley
 *
 */
@Configuration
public class RestAutoConfiguration {

	// @Bean
	public RestTemplate restTemplate() {
		return RestTemplateFactory.getInstance().createRealRestTemplate();
	}

}
