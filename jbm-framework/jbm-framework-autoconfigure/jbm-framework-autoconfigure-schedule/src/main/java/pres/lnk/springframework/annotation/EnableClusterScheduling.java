package pres.lnk.springframework.annotation;

import org.springframework.context.annotation.Import;
import pres.lnk.springframework.SchedulingClusterConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 启用集群环境的定时任务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingClusterConfiguration.class)
@Documented
public @interface EnableClusterScheduling {

}
