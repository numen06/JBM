package jbm.framework.spring.config.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ProloadConfig {

    String properties();
}
