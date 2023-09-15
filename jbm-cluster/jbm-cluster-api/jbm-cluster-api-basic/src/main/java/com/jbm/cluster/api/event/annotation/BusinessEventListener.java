package com.jbm.cluster.api.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wesley
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessEventListener {
    /**
     * 业务事件名称
     *
     * @return
     */
    Class eventClass();

    /**
     * 监听事件分组设置
     *
     * @return
     */
    String eventGroup() default "";


}
