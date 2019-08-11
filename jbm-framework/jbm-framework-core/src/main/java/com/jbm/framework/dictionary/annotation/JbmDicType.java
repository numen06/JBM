package com.jbm.framework.dictionary.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JbmDicType {

    String value() default "";

    String typeName() default "";

}
