package com.jbm.cluster.common.core.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 集群定时任务
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JbmClusterScheduled {

    /**
     * 定时规则
     */
    String cron();

    String description() default "";
}
