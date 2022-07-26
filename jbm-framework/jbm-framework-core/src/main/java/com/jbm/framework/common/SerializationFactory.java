package com.jbm.framework.common;

import java.io.IOException;

/**
 * 序列化接口
 *
 * @param <T>
 * @author wesley
 */
public interface SerializationFactory<T> {
    /**
     * 序列化
     *
     * @param obj
     * @return
     */
    byte[] serialize(T obj) throws IOException;

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    <E> E deserialize(final byte[] bytes, Class<E> clazz, E def);

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    T deserialize(byte[] bytes) throws IOException;

    /**
     * 注册序列化类
     *
     * @param clazz
     */
    void register(Class<?> clazz);

}
