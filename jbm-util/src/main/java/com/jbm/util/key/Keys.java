package com.jbm.util.key;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author wesley
 */
public class Keys {


    public static <T> KeyBean<T> ofBean(T value) {
        return new KeyBean<>(value);
    }

    public static <T> KeyBean<T> ofBean(Supplier<T> supplier) {
        return Keys.ofBean(supplier.get());
    }

    public static KeyObject of(Object value) {
        return new KeyObject(value);
    }

    public static KeyObject ofObj(String key, Object value) {
        return new KeyObject(key, value);
    }

    public static KeyArray ofArray(Object... keys) {
        return new KeyArray(keys);
    }

    public static IKey fromObj(String json) {
        return IKey.from(json);
    }

    public static KeyArray fromArray(String json) {
        return KeyArray.from(json);
    }
}
