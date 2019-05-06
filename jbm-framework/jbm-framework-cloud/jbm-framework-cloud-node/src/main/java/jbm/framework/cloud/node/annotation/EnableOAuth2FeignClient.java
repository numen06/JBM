package jbm.framework.cloud.node.annotation;

import jbm.framework.cloud.node.OAuth2FeignConfigure;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@EnableOAuth2Client
@Retention(RetentionPolicy.RUNTIME)
//@Import({OAuth2FeignConfigure.class})
@Documented
public @interface EnableOAuth2FeignClient {

}
