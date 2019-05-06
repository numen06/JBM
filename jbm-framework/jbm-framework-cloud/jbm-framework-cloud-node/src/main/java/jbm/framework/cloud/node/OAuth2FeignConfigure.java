package jbm.framework.cloud.node;

import feign.*;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import jbm.framework.cloud.node.interceptor.FeignAuth2RequestInterceptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.test.OAuth2ContextConfiguration;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.LinkedList;

import static feign.FeignException.errorStatus;
import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@AutoConfigureAfter({OAuth2ContextConfiguration.class, OAuth2ClientConfiguration.class})
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2FeignConfigure {
    // feign的OAuth2ClientContext
//    @Resource
    private OAuth2ClientContext feignOAuth2ClientContext = new DefaultOAuth2ClientContext();
    @Autowired
    private OAuth2ClientProperties credentials;

    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

//    @Bean
//    public OAuth2RestTemplate clientCredentialsRestTemplate() {
//        return new OAuth2RestTemplate(clientCredentialsResourceDetails);
//    }

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        ClientCredentialsResourceDetails clientCredentialsResourceDetails = new ClientCredentialsResourceDetails();
        clientCredentialsResourceDetails.setClientId("client");
        clientCredentialsResourceDetails.setClientSecret("123456");
        clientCredentialsResourceDetails.setAccessTokenUri("http://platform.hubao-tech.com/uaa/oauth/token");
        return new OAuth2FeignRequestInterceptor(feignOAuth2ClientContext, clientCredentialsResourceDetails);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }


    @Bean
    public Retryer retry() {
        // default Retryer will retry 5 times waiting waiting
        // 100 ms per retry with a 1.5* back off multiplier
        return new Retryer.Default(100, SECONDS.toMillis(1), 3);
    }


    @Bean
    public Decoder feignDecoder() {
        return new CustomResponseEntityDecoder(new SpringDecoder(this.messageConverters), feignOAuth2ClientContext);
    }


    /**
     * Http响应成功 但是token失效，需要定制 ResponseEntityDecoder
     *
     * @author maxianming
     * @date 2018/10/30 9:47
     */
    class CustomResponseEntityDecoder implements Decoder {
        private org.slf4j.Logger log = LoggerFactory.getLogger(CustomResponseEntityDecoder.class);

        private Decoder decoder;

        private OAuth2ClientContext context;

        public CustomResponseEntityDecoder(Decoder decoder, OAuth2ClientContext context) {
            this.decoder = decoder;
            this.context = context;
        }

        @Override
        public Object decode(final Response response, Type type) throws IOException, FeignException {
            if (log.isDebugEnabled()) {
                log.debug("feign decode type:{}，reponse:{}", type, response.body());
            }
            if (isParameterizeHttpEntity(type)) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
                Object decodedObject = decoder.decode(response, type);
                return createResponse(decodedObject, response);
            } else if (isHttpEntity(type)) {
                return createResponse(null, response);
            } else {
                // custom ResponseEntityDecoder if token is valid then go to errorDecoder
                String body = Util.toString(response.body().asReader());
                if (body.contains(OAuth2Exception.INVALID_TOKEN)) {
                    clearTokenAndRetry(response, body);
                }
                return decoder.decode(response, type);
            }
        }

        /**
         * token失效 则将token设置为null 然后重试
         *
         * @param
         * @return
         * @author maxianming
         * @date 2018/10/30 10:05
         */
        private void clearTokenAndRetry(Response response, String body) throws FeignException {
            log.error("接收到Feign请求资源响应,响应内容:{}", body);
            context.setAccessToken(null);
            throw new RetryableException("access_token过期，即将进行重试", new Date());
        }

        private boolean isParameterizeHttpEntity(Type type) {
            if (type instanceof ParameterizedType) {
                return isHttpEntity(((ParameterizedType) type).getRawType());
            }
            return false;
        }

        private boolean isHttpEntity(Type type) {
            if (type instanceof Class) {
                Class c = (Class) type;
                return HttpEntity.class.isAssignableFrom(c);
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private <T> ResponseEntity<T> createResponse(Object instance, Response response) {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            for (String key : response.headers().keySet()) {
                headers.put(key, new LinkedList<>(response.headers().get(key)));
            }
            return new ResponseEntity<>((T) instance, headers, org.springframework.http.HttpStatus.valueOf(response
                    .status()));
        }
    }


    @Bean
    public ErrorDecoder errorDecoder() {
        return new RestClientErrorDecoder(feignOAuth2ClientContext);
    }

    /**
     * Feign调用HTTP返回响应码错误时候，定制错误的解码
     *
     * @author maxianming
     * @date 2018/10/30 9:45
     */
    class RestClientErrorDecoder implements ErrorDecoder {
        private org.slf4j.Logger logger = LoggerFactory.getLogger(RestClientErrorDecoder.class);

        private OAuth2ClientContext context;

        RestClientErrorDecoder(OAuth2ClientContext context) {
            this.context = context;
        }

        public Exception decode(String methodKey, Response response) {
            logger.error("Feign调用异常，异常methodKey:{}, token:{}, response:{}", methodKey, context.getAccessToken(), response.body());
            if (HttpServletResponse.SC_UNAUTHORIZED == response.status()) {
                logger.error("接收到Feign请求资源响应401，access_token已经过期，重置access_token为null待重新获取。");
                context.setAccessToken(null);
                return new RetryableException("疑似access_token过期，即将进行重试", new Date());
            }
            return errorStatus(methodKey, response);
        }
    }


}