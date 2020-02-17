package jbm.framework.boot.autoconfigure.mvc;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        if (fastJsonHttpMessageConverter != null)
            converters.add(0, new FastJsonHttpMessageConverter());
        converters.add(0, new OAuth2AccessTokenMessageConverter());
    }

    /**
     * 多个WebSecurityConfigurerAdapter
     */
    @Configuration
    @Order(101)
    @ConditionalOnClass(javax.servlet.Filter.class)
    public static class ApiWebSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers(
                    "/error",
                    "/static/**",
                    "/v2/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/favicon.ico");
        }
    }


    /**
     * 资源处理器
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("swagger-ui.html", "doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


}
