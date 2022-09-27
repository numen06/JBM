package com.jbm.cluster.api.job;

import java.lang.annotation.*;

/**
 * @author wesley.zhang
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SchedulerJob {


    /**
     * 任务的名称
     * @return
     */
    String name();

    /**
     * 周期运行字符串
     * @return
     */
    String cron();

    /**
     * 是否启用
     * @return
     */
    boolean enable() default true;

}
