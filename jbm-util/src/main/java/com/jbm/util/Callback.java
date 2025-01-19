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
        return call(supplier, () -> defaultValue);
    }

    public static <T> T call(Supplier<Object> supplier, Supplier<T> defaultSupplier) {
        try {
            Object obj = supplier.get();
            return Objects.isNull(obj) ? defaultSupplier.get() : (T) obj;
        } catch (Exception e) {
            return defaultSupplier.get();
        }
    }
}
