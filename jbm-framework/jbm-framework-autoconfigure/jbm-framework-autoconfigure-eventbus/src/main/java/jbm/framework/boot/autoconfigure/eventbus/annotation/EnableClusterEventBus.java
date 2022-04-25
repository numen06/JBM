package jbm.framework.boot.autoconfigure.eventbus.annotation;

import jbm.framework.boot.autoconfigure.eventbus.autoconfigure.ClusterEventBusAutoConfigure;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author wesley
 * @create 2021/7/20 10:35 上午
 * @description eventbus function scan
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@Import({ClusterEventBusAutoConfigure.class})
public @interface EnableClusterEventBus {

    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

}
