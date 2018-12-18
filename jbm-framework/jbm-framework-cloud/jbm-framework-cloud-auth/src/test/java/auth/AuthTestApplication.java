package auth;

import com.jbm.framework.cloud.auth.feign.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;

@ComponentScan("com.jbm")
@SpringBootApplication
@EnableAuthorizationServer
@EnableFeignClients(basePackageClasses = UserService.class)
public abstract class AuthTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthTestApplication.class, args);
    }

}
