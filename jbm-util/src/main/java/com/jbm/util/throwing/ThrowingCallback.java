package com.jbm.util.throwing;

/**
 * @author wesley
 */
@FunctionalInterface
public interface ThrowingCallback<T> {
    T get() throws Exception;
}