package jbm.framework.boot.autoconfigure.mvc;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import jbm.framework.boot.autoconfigure.fastjson.FastJsonConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Slf4j
@Configuration
@AutoConfigureAfter({SecurityAutoConfiguration.class, FastJsonConfiguration.class})
@ConditionalOnClass(name = {"org.springframework.security.authentication.DefaultAuthenticationEventPublisher", "org.springframework.web.servlet.config.annotation.WebMvcConfigurer", "org.springframework.security.web.access.WebInvocationPrivilegeEvaluator"})
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        if (ObjectUtil.isNull(fastJsonHttpMessageConverter)) {
            fastJsonHttpMessageConverter = FastJsonConfiguration.getFastJsonHttpMessageConverter();
        }
        converters.add(0, fastJsonHttpMessageConverter);
//        converters.add(0, new OAuth2AccessTokenMessageConverter(fastJsonHttpMessageConverter));
    }


    /**
     * 资源处理器
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/", "file:static/");
        registry.addResourceHandler("swagger-ui.html", "doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


}
