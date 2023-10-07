package com.jbm.util.map;

import java.util.HashMap;
import java.util.Map;

public class ParameterMap<K, V> extends HashMap<K, V> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ParameterMap<K, V> append(K key, V value) {
        super.put(key, value);
        return this;
    }

    public ParameterMap<K, V> append(Map<? extends K, ? extends V> m) {
        super.putAll(m);
        return this;
    }

}
