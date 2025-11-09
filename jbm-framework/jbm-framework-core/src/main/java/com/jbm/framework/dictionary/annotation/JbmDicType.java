package com.jbm.framework.dictionary.annotation;

import lombok.Getter;

import java.lang.annotation.*;

/**
 * @author wesley
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JbmDicType {

    String value() default "";

    String typeName() default "";

}
