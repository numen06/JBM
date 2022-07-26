package pres.lnk.springframework.annotation;

import org.springframework.context.annotation.Import;
import pres.lnk.springframework.SchedulingClusterConfiguration;

import java.lang.annotation.*;


/**
 * 启用集群环境的定时任务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingClusterConfiguration.class)
@Documented
public @interface EnableClusterScheduling {

}
