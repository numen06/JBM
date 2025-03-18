package com.jbm.framework.boot.autoconfigure.retrofit;

import com.alibaba.fastjson.support.retrofit.Retrofit2ConverterFactory;
import com.github.lianjiatech.retrofit.spring.boot.config.RetrofitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 默认的缓存注入
 *
 * @author wesley
 */
@Configuration
@EnableCaching
@AutoConfigureAfter(com.github.lianjiatech.retrofit.spring.boot.config.RetrofitAutoConfiguration.class)
@EnableConfigurationProperties({RetrofitProperties.class, PlatformsProperties.class})
public class RetrofitAutoConfiguration {


    @Resource
    private RetrofitProperties retrofitProperties;

    @Resource
    private PlatformsProperties platformsProperties;

    @Bean
    public Retrofit2ConverterFactory retrofit2ConverterFactory() {
        return com.alibaba.fastjson.support.retrofit.Retrofit2ConverterFactory.create();
    }


    @Bean
    public StrategyFactory strategyFactory(@Autowired ApplicationContext applicationContext) {
        return new StrategyFactory(applicationContext, platformsProperties);
    }

//    @Bean
//    public SignatureInterceptor signatureInterceptor(StrategyFactory strategyFactory) {
//        return new SignatureInterceptor(strategyFactory, platformsProperties, retrofitProperties);
//    }
//
//    @Bean
//    public AuthInterceptor authInterceptor(StrategyFactory strategyFactory) {
//        return new AuthInterceptor(strategyFactory, platformsProperties, retrofitProperties);
//    }


}
