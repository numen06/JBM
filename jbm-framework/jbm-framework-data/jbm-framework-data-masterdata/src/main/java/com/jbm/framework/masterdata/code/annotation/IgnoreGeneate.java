package com.jbm.framework.masterdata.code.annotation;

import com.jbm.framework.masterdata.code.constants.CodeType;

import java.lang.annotation.*;

/**
 * @author: create by wesley
 * @date:2019/4/28
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreGeneate {

    CodeType[] value() default {};

}
