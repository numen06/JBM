package com.jbm.cluster.api.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wesley
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessEvent {

    /**
     * 业务事件名称
     *
     * @return
     */
    String name();

//    /**
//     * 分组
//     *
//     * @return
//     */
//    String group() default "SYSTEM";


    /**
     * 指定推送地址
     *
     * @return
     */
    String url() default "";

    /**
     * -1长期有效,时间单位为小时
     *
     * @return
     */
    long effectiveTime() default -1L;

    /**
     * 全局唯一,随机分配其一
     *
     * @return
     */
    boolean global() default false;

    /**
     * 描述信息
     *
     * @return
     */
    String description() default "";
}
