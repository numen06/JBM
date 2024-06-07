package com.jbm.cluster.common.basic.configuration;

import com.jbm.cluster.common.basic.configuration.config.TenantProperties;
import com.jbm.cluster.common.basic.interceptor.TenantInterceptor;
import com.jbm.cluster.common.basic.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

/**
 * @author fanscat
 * @createTime 2024/6/5 17:57
 */
@EnableConfigurationProperties({TenantProperties.class})
public class TenantConfiguration implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        Map<String, TenantService> map = this.applicationContext.getBeansOfType(TenantService.class);
        registry.addInterceptor(new TenantInterceptor(map.values())).addPathPatterns("/**");
    }
}
