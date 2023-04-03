package com.jbm.framework.opcua.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识OPC UA的心跳
 *
 * @author fanscat
 * @createTime 2023/3/31 17:08
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OpcUaHeartBeat {

    /**
     * 心跳的读取点位
     */
    String read();

    /**
     * 心跳的写入点位，读取到心跳值后写入
     */
    String write();
}
