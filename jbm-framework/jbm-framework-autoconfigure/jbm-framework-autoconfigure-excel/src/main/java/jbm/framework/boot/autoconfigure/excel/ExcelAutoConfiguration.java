package jbm.framework.boot.autoconfigure.excel;

import jbm.framework.excel.ExcelTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@EnableConfigurationProperties(ExcelProperties.class)
public class ExcelAutoConfiguration {

    @Bean
    public ExcelTemplate excelTemplate(ResourceLoader resourceLoader) {
        return new ExcelTemplate(resourceLoader);
    }

}
