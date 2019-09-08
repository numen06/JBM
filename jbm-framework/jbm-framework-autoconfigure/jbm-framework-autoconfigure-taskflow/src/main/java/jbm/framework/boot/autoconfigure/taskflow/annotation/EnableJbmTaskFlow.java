package jbm.framework.boot.autoconfigure.taskflow.annotation;

import jbm.framework.boot.autoconfigure.taskflow.JbmTaskFlowAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({JbmTaskFlowAutoConfiguration.class})
public @interface EnableJbmTaskFlow {

}
