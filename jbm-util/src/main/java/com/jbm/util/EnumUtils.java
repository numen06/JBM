package com.jbm.util;

import cn.hutool.core.util.EnumUtil;

public class EnumUtils extends EnumUtil {

    public static boolean equalsIgnoreCase(final Enum<?> e, String val) {
        return e.toString().equalsIgnoreCase(val);
    }

    public static boolean equals(final Enum<?> e, String val) {
        return e.toString().equals(val);
    }

    public static <E extends Enum<E>> boolean contains(final Class<E> enumClass, String val) {
        return EnumUtil.getEnumMap(enumClass).containsKey(val);
    }

    public static <E extends Enum<E>> boolean notContains(final Class<E> enumClass, String val) {
        return !contains(enumClass, val);
    }


}
