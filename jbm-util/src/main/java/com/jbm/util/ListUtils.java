package com.jbm.util;

import com.google.common.collect.Lists;
import com.jbm.util.comparator.MapComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * List的处理工具类
 *
 * @author Wesley
 */
public class ListUtils extends org.apache.commons.collections.ListUtils {

    private ListUtils() {
    }

    /**
     * 返回一个用逗号分开的字符串
     *
     * @param array
     * @return 1, 2, 3
     */
    public static <T> String toSimpleString(List<T> list) {
        return ArrayUtils.toSimpleString(list.toArray());
    }

    /**
     * String org.apache.commons.lang.ArrayUtils.toString(Object array)
     * <p>
     * <p>
     * <p>
     * Outputs an array as a String, treating null as an empty array.
     * <p>
     * Multi-dimensional arrays are handled correctly, including
     * multi-dimensional primitive arrays.
     * <p>
     * The format is that of Java source code, for example {a,b}.
     * <p>
     * Parameters: array the array to get a toString for, may be null Returns: a
     * String representation of the array, '{}' if null array input
     */
    public static <T> String toString(List<T> list) {
        return ArrayUtils.toString(list);
    }

    /**
     * String org.apache.commons.lang.ArrayUtils.toString(Object array, String
     * stringIfNull)
     * <p>
     * <p>
     * <p>
     * Outputs an array as a String handling nulls.
     * <p>
     * Multi-dimensional arrays are handled correctly, including
     * multi-dimensional primitive arrays.
     * <p>
     * The format is that of Java source code, for example {a,b}.
     * <p>
     * Parameters: array the array to get a toString for, may be null
     * stringIfNull the String to return if the array is null Returns: a String
     * representation of the array
     */
    public static <T> String toString(List<T> list, String stringIfNull) {
        return ArrayUtils.toString(list, stringIfNull);
    }

    /**
     * 新建一个ArrayList
     *
     * @return
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * 通过一个泛型接口初始化一个ArrayList
     *
     * @param list
     * @return
     */
    public static <E> ArrayList<E> newArrayList(List<E> list) {
        return new ArrayList<E>();
    }

    /**
     * 如果为空则简历一个新的ArrayList
     *
     * @param list
     * @return
     */
    public static <E> ArrayList<E> newArrayListIfNull(List<E> list) {
        return list == null ? new ArrayList<E>() : ObjectUtils.softCast(list, new ArrayList<E>());
    }

    /**
     * 通过数组建立一个ArrayList
     *
     * @param elements
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> newArrayList(E... elements) {
        ArrayList<E> list = new ArrayList<E>(ArrayUtils.getLength(elements));
        CollectionUtils.addAll(list, elements);
        return list;
    }

    /**
     * 对List<Map<String, Object>> resultList 按照Map中的key进行降序排序，
     *
     * @param resultList
     * @param key        排序字段名称
     * @param sortWay    排序方式：DESC 降序，ASC 升序 ；(默认升序)
     * @throws Exception
     */
    public static <K, V> void sort(List<Map<K, V>> list, final K key, final String sort) {
        try {
            Collections.sort(list, new MapComparator<Map<K, V>>(key, sort));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取最后一个值
     *
     * @param list 数据源
     * @return
     */
    public static <T> T last(List<T> list) {
        return last(list, null);
    }

    /**
     * 获取最后一个值
     *
     * <pre>
     * ListUtils.last(null, null) = null;
     * ListUtils.last(ListUtils.newArrayList(&quot;1&quot;, &quot;2&quot;, &quot;3&quot;), &quot;12&quot;) = 3;
     * </pre>
     *
     * @param list 数据源
     * @param def  默认值
     * @return
     */
    public static <T> T last(List<T> list, T def) {
        if (CollectionUtils.isEmpty(list)) {
            return def;
        }
        return list.get(list.size() - 1);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> push(List<T> list, T... elements) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.addAll(Lists.newArrayList(elements));
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> uniquePush(List<T> list, T... elements) {
        if (list == null) {
            list = new ArrayList<>();
        }
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null || list.contains(elements[i])) {
                continue;
            } else {
                list.add(elements[i]);
            }
        }
        return list;
    }

}
