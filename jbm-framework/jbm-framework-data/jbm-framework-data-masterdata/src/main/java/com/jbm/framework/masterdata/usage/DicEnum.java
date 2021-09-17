package com.jbm.framework.masterdata.usage;

import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-03 03:00
 **/
public interface DicEnum<K> {

    String DEFAULT_KEY_NAME = "key";

    String DEFAULT_VALUE_NAME = "value";

    default K getKey() {
        Field field = ReflectUtil.getField(this.getClass(), DEFAULT_KEY_NAME);
        if (field == null)
            return null;
        try {
            field.setAccessible(true);
            return (K) field.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    default String getValue() {
        Field field = ReflectUtil.getField(this.getClass(), DEFAULT_VALUE_NAME);
        if (field == null)
            return null;
        try {
            field.setAccessible(true);
            return field.get(this).toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends Enum<T>> T valueOfEnum(Class<T> enumClass, T value) {
        if (value == null)
            throw new IllegalArgumentException("DicEnum value should not be null");
        if (enumClass.isAssignableFrom(DicEnum.class))
            throw new IllegalArgumentException("illegal DicEnum type");
        T[] enums = enumClass.getEnumConstants();
        for (T t : enums) {
            DicEnum displayedEnum = (DicEnum) t;
            if (displayedEnum.getValue().equals(value))
                return (T) displayedEnum;
        }
        throw new IllegalArgumentException("cannot parse integer: " + value + " to " + enumClass.getName());
    }
}
