package jbm.framework.cloud.node.annotation;

import jbm.framework.cloud.node.CrossDomainConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(CrossDomainConfiguration.class)
@Documented
public @interface EnableCrossDomain {

}
