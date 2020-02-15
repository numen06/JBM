package jbm.framework.boot.autoconfigure.fegin;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.springfox.SwaggerJsonSerializer;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * Feign OAuth2 request interceptor.
 *
 * @author wesley.zhang
 */
@Slf4j
@ConditionalOnClass(name = "feign.codec.Encoder")
public class FeignAutoConfiguration {
    public static int connectTimeOutMillis = 12000;
    public static int readTimeOutMillis = 12000;

    @Autowired
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    @Bean
    public Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        Encoder encoder = new FeignSpringFormEncoder(new SpringEncoder(feignHttpMessageConverter(messageConverters)));
        log.info("FeignSpringFormEncoder [{}]", encoder);
        return encoder;
    }

    @Bean
    public ResponseEntityDecoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new ResponseEntityDecoder(new SpringDecoder(feignHttpMessageConverter(messageConverters)));
    }

    @Bean
    @ConditionalOnMissingBean(FeignRequestInterceptor.class)
    public RequestInterceptor feignRequestInterceptor() {
        FeignRequestInterceptor interceptor = new FeignRequestInterceptor();
        log.info("FeignRequestInterceptor [{}]", interceptor);
        return interceptor;
    }

    private ObjectFactory<HttpMessageConverters> feignHttpMessageConverter(ObjectFactory<HttpMessageConverters> messageConverters) {
        final HttpMessageConverters httpMessageConverters =
                new HttpMessageConverters(fastJsonHttpMessageConverter);
        return () -> httpMessageConverters;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(connectTimeOutMillis, readTimeOutMillis);
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default();
    }

}
