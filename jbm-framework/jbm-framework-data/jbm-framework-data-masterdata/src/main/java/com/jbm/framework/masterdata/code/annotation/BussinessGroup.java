package com.jbm.framework.masterdata.code.annotation;


import java.lang.annotation.*;

/**
 * 业务分组
 *
 * @author wesley
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BussinessGroup {

    String businessName();

    String[] businessNames() default {};


}
