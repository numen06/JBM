package com.jbm.cluster.common.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("clusterFeignAutoConfiguration")
public class FeignAutoConfiguration {

    @Bean
    public RequestInterceptor clusterRequestInterceptor() {
        return template -> template.header("X-Test", "cluster");
    }

    @Bean
    public Object appPreRequestInterceptor() {
        return new Object();
    }
}
