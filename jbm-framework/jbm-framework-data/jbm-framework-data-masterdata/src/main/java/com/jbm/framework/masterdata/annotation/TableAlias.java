package com.jbm.framework.masterdata.annotation;

import java.lang.annotation.*;

/**
 * table别名
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TableAlias {
    String value();
}
