package jbm.framework.boot.autoconfigure.swagger.annotation;

import jbm.framework.boot.autoconfigure.swagger.SwaggerAutoConfiguration;
import org.springframework.context.annotation.Import;
import springfox.documentation.swagger2.configuration.Swagger2DocumentationConfiguration;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({SwaggerAutoConfiguration.class, Swagger2DocumentationConfiguration.class})
public @interface EnableSwagger2 {

}
