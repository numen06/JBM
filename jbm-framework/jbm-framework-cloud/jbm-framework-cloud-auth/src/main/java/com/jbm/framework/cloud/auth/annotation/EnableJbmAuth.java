package com.jbm.framework.cloud.auth.annotation;

import com.jbm.framework.cloud.auth.config.AuthorizationServerConfig;
import com.jbm.framework.cloud.auth.config.ResourceServerConfiguration;
import com.jbm.framework.cloud.auth.config.WebSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@EnableResourceServer
@Retention(RetentionPolicy.RUNTIME)
@Import({WebSecurityConfig.class, AuthorizationServerConfig.class, ResourceServerConfiguration.class})
@Documented
public @interface EnableJbmAuth {

}
