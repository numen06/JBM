package com.jbm.util.sensitive;

import java.lang.annotation.*;

/**
 * 标记响应中需要脱敏的字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveField {
    SensitiveType value();
}
