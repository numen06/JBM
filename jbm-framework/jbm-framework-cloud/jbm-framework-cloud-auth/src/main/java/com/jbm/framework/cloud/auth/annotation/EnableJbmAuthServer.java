package com.jbm.framework.cloud.auth.annotation;

import com.jbm.framework.cloud.auth.config.AuthorizationServerConfig;
import com.jbm.framework.cloud.auth.config.ResourceServerConfiguration;
import com.jbm.framework.cloud.auth.config.WebSecurityConfig;
import com.jbm.framework.cloud.auth.feign.UserService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({WebSecurityConfig.class, AuthorizationServerConfig.class, ResourceServerConfiguration.class})
@EnableResourceServer
@ComponentScan("com.jbm")
@EnableFeignClients(basePackageClasses = UserService.class)
@SpringBootApplication
public @interface EnableJbmAuthServer {

}
