package com.jbm.framework.opcua.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将字段标识为读取字段
 *
 * @author fanscat
 * @createTime 2023/3/31 17:08
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OpcUaReadField {
    /**
     * 点位别名
     */
    String alias();
}
