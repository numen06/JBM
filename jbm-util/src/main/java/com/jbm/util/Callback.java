package com.jbm.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 回调方法
 * @author wesley
 */
public class Callback {

    public static <T> T call(Supplier<Object> supplier) {
        return call(supplier, null);
    }

    public static <T> T call(Supplier<Object> supplier, T defaultValue) {
        try {
            Object obj = supplier.get();
            return Objects.isNull(obj) ? defaultValue : (T) obj;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
