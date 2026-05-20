package com.jbm.examples.extendfield.business.web.config;

import com.jbm.examples.extendfield.business.web.filter.DemoTenantIdHeaderFilter;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DemoTenantHeaderWebConfiguration {

    @Bean
    public FilterRegistrationBean<DemoTenantIdHeaderFilter> demoTenantIdHeaderFilter(
            ExtendFieldProperties extendFieldProperties) {
        FilterRegistrationBean<DemoTenantIdHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new DemoTenantIdHeaderFilter(extendFieldProperties));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }
}
