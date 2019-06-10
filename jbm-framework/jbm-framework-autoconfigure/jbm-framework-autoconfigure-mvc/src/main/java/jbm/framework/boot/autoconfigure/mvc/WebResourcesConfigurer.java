package jbm.framework.boot.autoconfigure.mvc;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;

@Configuration
public class WebResourcesConfigurer extends WebMvcConfigurerAdapter {

    private static final Log logger = LogFactory.getLog(WebResourcesConfigurer.class);
    // private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
    // "classpath:/META-INF/resources/", "classpath:/resources/",
    // "classpath:/static/", "classpath:/public/" };
    //
    // @Autowired
    // private ApplicationContext applicationContext;

    @Autowired
    private FormattingConversionService conversionService;

    @Primary
    @Bean
    public GlobalDefultExceptionHandler globalDefultExceptionHandler() {
        return new GlobalDefultExceptionHandler();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        List<HttpMessageConverter<?>> ss = new ArrayList<HttpMessageConverter<?>>();
        for (HttpMessageConverter<?> httpMessageConverter : converters) {
            if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
                // converters.remove(httpMessageConverter);
                ss.add(httpMessageConverter);
            }
        }
        for (HttpMessageConverter<?> httpMessageConverter : ss) {
            converters.remove(httpMessageConverter);
        }
        converters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
        FastJsonHttpMessageConverter4 fastConverter = new SwaggerFastJsonHttpMessageConverter4();
        converters.add(fastConverter);


        conversionService.addConverter(String.class, Date.class, new Converter<String, Date>() {
            @Override
            public Date convert(String source) {
                return com.jbm.util.TimeUtils.softParseDate(source);
            }
        });
    }

    // @Override
    // public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // // 启动的绝对目录
    // // String runPath = FileUtils.getFile("").getAbsolutePath();
    // // StringBuilder sb = new
    // // StringBuilder().append(runPath).append("/META-INF/resources/");
    // // PathResource resource = new PathResource(sb.toString());
    // try {
    // String targetClassPath =
    // applicationContext.getResource("classpath:META-INF/resources/").getURI().toString();
    // String sourceClassPath = StringUtils.remove(targetClassPath,
    // "/target/classes");
    // Resource resource = applicationContext.getResource(sourceClassPath);
    // if (resource.getFile().exists() &&
    // !"jar:".equals(StringUtils.left(sourceClassPath, 4))) {
    // // String projectPath = "file:" + resource.getPath() + "\\";
    // //
    // registry.addResourceHandler("/**").addResourceLocations(projectPath).addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
    // // logger.info("start project path is " + projectPath);
    // registry.addResourceHandler("/**").addResourceLocations(resource.getURI().toString()).addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
    // logger.info("develop resource path is " + resource.getURI().toString());
    // }
    // } catch (IOException e) {
    // logger.warn("develop resource path is error");
    // }
    // //
    // registry.addResourceHandler("/app/**").addResourceLocations("classpath:/META-INF/app/");
    // //
    // registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/META-INF/assets/");
    // //
    // registry.addResourceHandler("/lib/**").addResourceLocations("classpath:/META-INF/lib/");
    // super.addResourceHandlers(registry);
    // }

}
