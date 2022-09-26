package com.jbm.cluster.api.job;

import java.lang.annotation.*;

/**
 * @author wesley.zhang
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SchedulerJob {


    String name();

    String cron();

    boolean enable() default true;

}
