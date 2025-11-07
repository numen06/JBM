package jbm.framework.boot.autoconfigure.swagger.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * swagger 资源映射路径
 *
 * @author wesley.zhang
 */
@Configuration
public class SwaggerWebConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    private SwaggerProperties swaggerProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /** swagger-ui 地址 */
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
    }

    /**
     * 注册Swagger基础认证过滤器
     */
    @Bean
    @ConditionalOnBean(SwaggerProperties.class)
    public FilterRegistrationBean<SwaggerBasicAuthFilter> swaggerBasicAuthFilter() {
        FilterRegistrationBean<SwaggerBasicAuthFilter> registration = new FilterRegistrationBean<>();
        SwaggerBasicAuthFilter filter = new SwaggerBasicAuthFilter(swaggerProperties.getBasicAuth());
        registration.setFilter(filter);
        registration.addUrlPatterns("/swagger-ui/**", "/v2/api-docs/**", "/swagger-resources/**", "/swagger-ui.html");
        registration.setName("swaggerBasicAuthFilter");
        registration.setOrder(1);
        return registration;
    }
}