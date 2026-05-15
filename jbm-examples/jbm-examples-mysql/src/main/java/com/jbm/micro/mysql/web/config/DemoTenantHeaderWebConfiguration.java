package com.jbm.micro.mysql.web.config;

import com.jbm.micro.mysql.web.filter.DemoTenantIdHeaderFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DemoTenantHeaderWebConfiguration {

    @Bean
    public FilterRegistrationBean<DemoTenantIdHeaderFilter> demoTenantIdHeaderFilterRegistration() {
        FilterRegistrationBean<DemoTenantIdHeaderFilter> bean = new FilterRegistrationBean<>(new DemoTenantIdHeaderFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
