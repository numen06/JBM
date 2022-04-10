package com.jbm.cluster.node.configuration.annotation;

import com.jbm.cluster.node.configuration.cluster.JbmClusterEventRegistry;
import com.jbm.cluster.node.configuration.cluster.JbmClusterScheduledRegistry;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({JbmClusterEventRegistry.class, JbmClusterScheduledRegistry.class})
@Documented
@Inherited
public @interface EnableJbmCluster {

    @AliasFor("targetPackages")
    String[] value() default {};

    @AliasFor("value")
    String[] targetPackages() default {};

    Class<?>[] targetPackageClasses() default {};

    String targetPackage() default "*";
}
