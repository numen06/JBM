package com.jbm.util.map;

import com.jbm.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

public class SimpleKey implements Serializable {

    public static final SimpleKey EMPTY = new SimpleKey();
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final Object[] params;
    private final int hashCode;

    /**
     * Create a new {@link SimpleKey} instance.
     *
     * @param elements the elements of the key
     */
    public SimpleKey(Object... elements) {
        this.params = new Object[elements.length];
        System.arraycopy(elements, 0, this.params, 0, elements.length);
        this.hashCode = Arrays.deepHashCode(this.params);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj || (obj instanceof SimpleKey && Arrays.deepEquals(this.params, ((SimpleKey) obj).params)));
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + StringUtils.join(this.params, ",") + "]";
    }

}