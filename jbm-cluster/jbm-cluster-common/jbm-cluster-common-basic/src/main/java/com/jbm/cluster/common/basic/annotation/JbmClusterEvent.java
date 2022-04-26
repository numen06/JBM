package com.jbm.cluster.common.basic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 集群回调注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JbmClusterEvent {

    /**
     * 事件类型
     */
    String eventType() default "system";


    /**
     * 事件类型类
     */
    Class eventTypeClass() default JbmClusterEvent.class;

}
