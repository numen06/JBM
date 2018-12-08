package jbm.framework.boot.autoconfigure.excel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import jbm.framework.excel.ExcelTemplate;

@EnableConfigurationProperties(ExcelProperties.class)
public class ExcelAutoConfiguration {

	@Bean
	public ExcelTemplate beetlTemplate(ResourceLoader resourceLoader) {
		return new ExcelTemplate(resourceLoader);
	}

}
