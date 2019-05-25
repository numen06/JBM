package jbm.framework.cloud.node.annotation;

import jbm.framework.cloud.node.NodeConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@EnableResourceServer
@Retention(RetentionPolicy.RUNTIME)
@Import({NodeConfiguration.class})
@Documented
public @interface EnableJbmNode {

}
