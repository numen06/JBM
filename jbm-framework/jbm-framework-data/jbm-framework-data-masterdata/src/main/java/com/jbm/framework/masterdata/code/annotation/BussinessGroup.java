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


    Class businessClass();

//    String[] businessNames() default {};


}
