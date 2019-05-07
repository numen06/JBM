package jbm.framework.cloud.node.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
//@EnableOAuth2Client
@Retention(RetentionPolicy.RUNTIME)
//@Import({OAuth2FeignConfigure.class})
@Documented
public @interface EnableOAuth2FeignClient {

}
