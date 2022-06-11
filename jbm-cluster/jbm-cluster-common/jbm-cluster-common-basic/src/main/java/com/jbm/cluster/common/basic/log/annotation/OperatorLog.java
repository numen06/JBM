package com.jbm.cluster.common.basic.log.annotation;

import com.jbm.cluster.common.basic.log.model.BusinessType;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 *
 * @author ruoyi
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperatorLog {
    /**
     * 模块
     */
    String title() default "";

    /**
     * 功能
     */
    BusinessType businessType() default BusinessType.OTHER;

}
