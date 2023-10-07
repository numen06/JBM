package com.jbm.util.comparator;

public class ComparatorFactory {

    @SuppressWarnings("unchecked")
    public static <T> AbstractComparator<T> creatComparator(Class<T> clazz, String sort) {
        if (Double.class == clazz)
            return (AbstractComparator<T>) new DoubleComparator(sort);
        if (Integer.class == clazz)
            return (AbstractComparator<T>) new IntegerComparator(sort);
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> AbstractComparator<T> creatComparator(T obj) {
        return (AbstractComparator<T>) creatComparator(obj.getClass(), AbstractComparator.ASC);
    }

    @SuppressWarnings("unchecked")
    public static <T> AbstractComparator<T> creatComparator(T obj, String sort) {
        return (AbstractComparator<T>) creatComparator(obj.getClass(), sort);
    }
}
