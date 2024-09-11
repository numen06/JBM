package com.jbm.util.key;

/**
 * @author wesley
 */
public class Keys {

    public static KeyObject ofObj(String key, Object value) {
        return new KeyObject(key, value);
    }

    public static KeyArray ofArray(Object... keys) {
        return new KeyArray(keys);
    }

    public static KeyObject fromObj(String json) {
        return KeyObject.from(json);
    }

    public static KeyArray fromArray(String json) {
        return KeyArray.from(json);
    }
}
