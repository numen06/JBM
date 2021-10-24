package com.jbm.cluster.node.configuration;

import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.common.annotation.RequestMappingScan;
import com.jbm.cluster.common.configuration.JbmClusterProperties;
import com.jbm.cluster.common.configuration.JbmIdGenProperties;
import com.jbm.cluster.common.configuration.JbmScanProperties;
import com.jbm.cluster.common.exception.OAuth2ExceptionHandler;
import com.jbm.cluster.common.exception.OpenRestResponseErrorHandler;
import com.jbm.cluster.common.filter.XFilter;
import com.jbm.cluster.common.health.DbHealthIndicator;
import com.jbm.cluster.common.security.OpenUserConverter;
import com.jbm.cluster.common.security.http.OpenRestTemplate;
import com.jbm.cluster.common.security.oauth2.client.JbmOAuth2ClientProperties;
import com.jbm.cluster.node.configuration.cluster.ClusterDicScan;
import com.jbm.cluster.node.configuration.fegin.FeignRequestOAuth2Interceptor;
import jbm.framework.boot.autoconfigure.fegin.FeignRequestInterceptor;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.web.client.RestTemplate;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2020-02-05 20:52
 **/
@Slf4j
//@Configuration
@EnableConfigurationProperties({JbmClusterProperties.class, JbmIdGenProperties.class, JbmOAuth2ClientProperties.class, JbmScanProperties.class})
public class NodeConfiguration {

    @Bean
    @ConditionalOnMissingBean(JbmScanProperties.class)
    public JbmScanProperties scanProperties() {
        return new JbmScanProperties();
    }

    /**
     * xss过滤
     * body缓存
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean XssFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new XFilter());
        log.info("XFilter [{}]", filterRegistrationBean);
        return filterRegistrationBean;
    }

    /**
     * 默认加密配置
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(BCryptPasswordEncoder.class)
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        log.info("BCryptPasswordEncoder [{}]", encoder);
        return encoder;
    }


    /**
     * Spring上下文工具配置
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(SpringContextHolder.class)
    public SpringContextHolder springContextHolder() {
        SpringContextHolder holder = new SpringContextHolder();
        log.info("SpringContextHolder [{}]", holder);
        return holder;
    }


    /**
     * 全局异常处理配置
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2ExceptionHandler.class)
    public OAuth2ExceptionHandler oAuth2ExceptionHandler() {
        OAuth2ExceptionHandler exceptionHandler = new OAuth2ExceptionHandler();
        log.info("OpenGlobalExceptionHandler [{}]", exceptionHandler);
        return exceptionHandler;
    }

    /**
     * 自定义Oauth2请求类
     *
     * @param openCommonProperties
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(OpenRestTemplate.class)
    public OpenRestTemplate openRestTemplate(JbmClusterProperties openCommonProperties, BusProperties busProperties, ApplicationEventPublisher publisher) {
        OpenRestTemplate restTemplate = new OpenRestTemplate(openCommonProperties, busProperties, publisher);
        //设置自定义ErrorHandler
        restTemplate.setErrorHandler(new OpenRestResponseErrorHandler());
        log.info("OpenRestTemplate [{}]", restTemplate);
        return restTemplate;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        //设置自定义ErrorHandler
        restTemplate.setErrorHandler(new OpenRestResponseErrorHandler());
        log.info("RestTemplate [{}]", restTemplate);
        return restTemplate;
    }

    @Bean
    @Primary
    public FeignRequestInterceptor feignRequestInterceptor(OpenRestTemplate openRestTemplate) {
        FeignRequestInterceptor interceptor = new FeignRequestOAuth2Interceptor(openRestTemplate.buildOAuth2ClientRequest());
        log.info("FeignRequestOAuth2Interceptor [{}]", interceptor);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(DbHealthIndicator.class)
    public DbHealthIndicator dbHealthIndicator() {
        DbHealthIndicator dbHealthIndicator = new DbHealthIndicator();
        log.info("DbHealthIndicator [{}]", dbHealthIndicator);
        return dbHealthIndicator;
    }


    /**
     * 自定义注解扫描
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(RequestMappingScan.class)
    public RequestMappingScan resourceAnnotationScan(AmqpTemplate amqpTemplate, JbmScanProperties scanProperties) {
        RequestMappingScan scan = new RequestMappingScan(amqpTemplate, scanProperties);
        log.info("RequestMappingScan [{}]", scan);
        return scan;
    }

    @Bean
    @ConditionalOnBean(DictionaryTemplate.class)
    public ClusterDicScan clusterDicScan(AmqpTemplate amqpTemplate, DictionaryTemplate dictionaryTemplate) {
        ClusterDicScan scan = new ClusterDicScan(dictionaryTemplate, amqpTemplate);
        log.info("ClusterDicScan [{}]", scan);
        return scan;
    }

    /**
     * 构建token转换器
     *
     * @return
     */
    @Bean
    @Primary
    public DefaultAccessTokenConverter accessTokenConverter() {
        OpenUserConverter userAuthenticationConverter = new OpenUserConverter();
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        accessTokenConverter.setUserTokenConverter(userAuthenticationConverter);
        return accessTokenConverter;
    }

}
