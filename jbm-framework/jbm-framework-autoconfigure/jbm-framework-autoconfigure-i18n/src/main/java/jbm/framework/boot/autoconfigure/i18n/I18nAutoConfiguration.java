package jbm.framework.boot.autoconfigure.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 默认的Mongo注入
 * 
 * @author wesley
 *
 */
@Configuration
public class I18nAutoConfiguration {

	@Bean
	public MessageSourceTamplate messageSourceTamplate() {
		return new MessageSourceTamplate();
	}
}
