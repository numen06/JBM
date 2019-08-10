package com.jbm.autoconfig.dic.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JbmDicType {

    String value() default "";

}
