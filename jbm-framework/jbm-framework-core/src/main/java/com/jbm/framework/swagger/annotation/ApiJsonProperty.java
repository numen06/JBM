package com.jbm.framework.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * swagger map属性注解
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiJsonProperty {

    String key() default ""; // key

    Class type() default String.class; // 支持string、int、double

    String description() default "";// 参数描述


    boolean required() default false; // 是否必传

}
