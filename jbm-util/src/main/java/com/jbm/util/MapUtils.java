package com.jbm.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.jbm.util.map.ParameterMap;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Map工具类<br>
 *
 * @author Wesley<br>
 */
public class MapUtils extends org.apache.commons.collections.MapUtils {
    private MapUtils() {
    }

    /**
     * 新建一个参数类型的Map
     *
     * @return
     */
    public static ParameterMap<String, Object> newParamMap() {
        return new ParameterMap<String, Object>();
    }

    /**
     * 新建一个参数形的Map
     *
     * @param key
     * @param value
     * @return
     */
    public static ParameterMap<String, Object> newParamMap(String key, Object value) {
        ParameterMap<String, Object> map = new ParameterMap<String, Object>();
        map.put(key, value);
        return map;
    }

    public static int generateKey(Object... elements) {
        final Object[] params = new Object[elements.length];
        System.arraycopy(elements, 0, params, 0, elements.length);
        final int hashCode = Arrays.deepHashCode(params);
        return hashCode;
    }

    /**
     * 新建一个HashMap
     *
     * @return
     */
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    public static <K, V> HashMap<K, V> newHashMap(K key, V value) {
        HashMap<K, V> map = new HashMap<K, V>();
        map.put(key, value);
        return map;
    }

    public static <K, V> boolean containsKey(Map<K, V> map, K key) {
        if (MapUtils.isNotEmpty(map)) {
            return map.containsKey(key);
        }
        return false;
    }

    /**
     * 将符合【one=1,two=2,three=3】格式的字符串分割成为一个Map对象
     *
     * <pre>
     * MapUtils.split('one=1,two=2,three=3',null) = {one=1,two=2,three=3};
     * MapUtils.split(null,{one=1})               = {one=1};
     * </pre>
     *
     * @param source 需要分割的字符串
     * @param def    分割错误之后的默认值
     * @return 分割后的Map
     */
    public static Map<String, String> split(String source, Map<String, String> def) {
        return split(source, def, ",", "=");
    }

    /**
     * 将格式字符串分割成为一个Map对象
     *
     * <pre>
     * MapUtils.split('one:1;two:2;three:3',null,';',':') = {one=1,two=2,three=3};
     * </pre>
     *
     * @param source 需要分割的字符串
     * @param def    分割错误之后的默认值
     * @param s1     分割对象的分隔符
     * @param s2     分割键值对的分隔符
     * @return
     */
    public static Map<String, String> split(String source, Map<String, String> def, final String s1, final String s2) {
        Map<String, String> result = def;
        try {
            if (StringUtils.isBlank(source))
                return def;
            String temp = StringUtils.trim(source);
            if (s1.equals(StringUtils.right(temp, 1)))
                temp = StringUtils.left(temp, StringUtils.length(temp) - 1);
            result = Splitter.on(s1).trimResults().withKeyValueSeparator(s2).split(temp);
        } catch (Exception e) {
            return def;
        }
        return result;
    }

    public static Map<String, String> split(String source) {
        Map<String, String> map = Maps.newHashMap();
        return split(source, map, ";", ":");
    }

    /**
     * 将Map转换为Object
     *
     * @param clazz 目标对象的类
     * @param map   待转换Map
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static <T, V> T toObject(Class<T> clazz, Map<String, V> map)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        T object = clazz.newInstance();
        return toObject(object, map);
    }

    /**
     * 将Map转换为Object
     *
     * @param clazz       目标对象的类
     * @param map         待转换Map
     * @param toCamelCase 是否去掉下划线
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static <T, V> T toObject(Class<T> clazz, Map<String, V> map, boolean toCamelCase)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        T object = clazz.newInstance();
        return toObject(object, map, toCamelCase);
    }

    /**
     * 将Map转换为Object
     *
     * @param object 目标对象
     * @param map    待转换Map
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static <T, V> T toObject(T object, Map<String, V> map)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return toObject(object, map, false);
    }

    public static <T, V> T toObject(T object, Map<String, V> map, boolean toCamelCase)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (toCamelCase)
            map = toCamelCaseMap(map);
        BeanUtils.populate(object, map);
        return object;
    }

    /**
     * 将MAP转换为对象List
     *
     * <pre>
     * MapUtils.toList("1=1,2=2")=[{value=1, key=1}, {value=2, key=2}];
     * </pre>
     *
     * @param map
     * @return
     */
    public static <E, K, V> List<Map<String, Object>> toList(Map<K, V> map) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (MapUtils.isEmpty(map))
            return list;
        for (K key : map.keySet()) {
            Map<String, Object> obj = new HashMap<String, Object>();
            obj.put("key", key);
            obj.put("value", map.get(key));
            list.add(obj);
        }
        return list;
    }

    /**
     * 对象转Map
     *
     * @param object 目标对象
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static Map<String, String> toMap(Object object)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return BeanUtils.describe(object);
    }

    /**
     * 转换为Collection<Map<K, V>>
     *
     * @param collection 待转换对象集合
     * @return 转换后的Collection<Map < K, V>>
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static <T> Collection<Map<String, String>> toMapList(Collection<T> collection)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<Map<String, String>> retList = new ArrayList<Map<String, String>>();
        if (collection != null && !collection.isEmpty()) {
            for (Object object : collection) {
                Map<String, String> map = toMap(object);
                retList.add(map);
            }
        }
        return retList;
    }

    /**
     * 转换为Collection,同时为字段做驼峰转换<Map<K, V>>
     *
     * @param collection 待转换对象集合
     * @return 转换后的Collection<Map < K, V>>
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static <T> Collection<Map<String, String>> toMapListForFlat(Collection<T> collection)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<Map<String, String>> retList = new ArrayList<Map<String, String>>();
        if (collection != null && !collection.isEmpty()) {
            for (Object object : collection) {
                Map<String, String> map = toMapForFlat(object);
                retList.add(map);
            }
        }
        return retList;
    }

    /**
     * 转换成Map并提供字段命名驼峰转平行
     *
     * @param clazz  目标对象所在类
     * @param object 目标对象
     * @param map    待转换Map
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Map<String, String> toMapForFlat(Object object)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<String, String> map = toMap(object);
        return toUnderlineStringMap(map);
    }

    /**
     * 将Map的Keys去下划线<br>
     * (例:branch_no -> branchNo )<br>
     *
     * @param map 待转换Map
     * @return
     */
    public static <V> Map<String, V> toCamelCaseMap(Map<String, V> map) {
        Map<String, V> newMap = newHashMap();
        if (MapUtil.isEmpty(map))
            return newMap;
        for (String key : map.keySet()) {
            safeAddToMap(newMap, JavaBeanUtil.toCamelCaseString(key), map.get(key));
        }
        return newMap;
    }

    /**
     * 将Map的Keys去下划线<br>
     * (例:branchNo -> branch_no)<br>
     *
     * @param map 待转换Map
     * @return
     */
    public static <V> Map<String, V> toSnakeCaseMap(Map<String, V> map) {
        Map<String, V> newMap = newHashMap();
        if (MapUtil.isEmpty(map))
            return newMap;
        map.forEach(new BiConsumer<String, V>() {
            @Override
            public void accept(String key, V value) {
                safeAddToMap(newMap, StrUtil.toSymbolCase(key, StrUtil.C_UNDERLINE), value);
            }
        });
        return newMap;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void safeAddToMap(Map map, Object key, Object value) throws NullPointerException {
        if (map == null)
            return;
        map.put(key, value);
    }

    /**
     * 将对象安全的加入map中,如果是null值,忽略加入Map
     *
     * @param map
     * @param key
     * @param value
     */
    public static <K, V> void safeAddToMapIgnoreNull(Map<K, V> map, K key, V value) {
        if (value == null)
            return;
        safeAddToMap(map, key, value);
    }

    /**
     * 将Map的Keys转译成下划线格式的<br>
     * (例:branchNo -> branch_no)<br>
     *
     * @param map 待转换Map
     * @return
     */
    public static <V> Map<String, V> toUnderlineStringMap(Map<String, V> map) {
        Map<String, V> newMap = new HashMap<String, V>();
        for (String key : map.keySet()) {
            newMap.put(JavaBeanUtil.toUnderlineString(key), map.get(key));
        }
        return newMap;
    }

    public static Map<String, Object> toMapIgnoreNull(Object bean)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return toMapIgnoreNull(new HashMap<String, Object>(), bean);
    }

    /**
     * 将javaBean对象转换成map</br>
     * 忽略空字符
     *
     * @param bean
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static Map<String, Object> toMapIgnoreNull(Map<String, Object> map, Object bean)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (bean instanceof DynaBean) {
            DynaProperty[] descriptors = ((DynaBean) bean).getDynaClass().getDynaProperties();
            for (int i = 0; i < descriptors.length; i++) {
                String name = descriptors[i].getName();
                if (StringUtils.equals("class", name))
                    continue;
                safeAddToMapIgnoreNull(map, name, PropertyUtils.getProperty(bean, name));
            }
        } else {
            PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(bean);
            for (int i = 0; i < descriptors.length; i++) {
                String name = descriptors[i].getName();
                if (StringUtils.equals("class", name))
                    continue;
                if (descriptors[i].getReadMethod() != null) {
                    safeAddToMapIgnoreNull(map, name, PropertyUtils.getProperty(bean, name));
                }
            }
        }
        return (map);
    }

    /**
     * 如果是字符串，将抛去前后去除为空的字符串
     *
     * @param map
     * @param key
     * @param value
     */
    public static <K, V> void safeAddToMapIgnoreTrimToNull(Map<K, V> map, K key, V value) {
        if (value == null)
            return;
        if (value instanceof String && StringUtils.isBlank((String) value))
            return;
        safeAddToMap(map, key, value);
    }

    /**
     * 如果是字符串，将抛去前后去除为空的字符串
     *
     * @param map
     * @param bean
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static Map<String, Object> toMapIgnoreTrimToNull(Map<String, Object> map, Object bean)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (bean instanceof DynaBean) {
            DynaProperty[] descriptors = ((DynaBean) bean).getDynaClass().getDynaProperties();
            for (int i = 0; i < descriptors.length; i++) {
                String name = descriptors[i].getName();
                if (StringUtils.equals("class", name))
                    continue;
                safeAddToMapIgnoreTrimToNull(map, name, PropertyUtils.getProperty(bean, name));
            }
        } else {
            PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(bean);
            for (int i = 0; i < descriptors.length; i++) {
                String name = descriptors[i].getName();
                if (StringUtils.equals("class", name))
                    continue;
                if (descriptors[i].getReadMethod() != null) {
                    safeAddToMapIgnoreTrimToNull(map, name, PropertyUtils.getProperty(bean, name));
                }
            }
        }
        return (map);
    }

    /**
     * 将一个map安全的加入到另一map中
     *
     * @param source
     * @param map
     * @return
     */
    public static <K, V> Map<K, V> putAll(Map<K, V> source, Map<K, V> map) {
        if (map == null || source == null)
            return source;
        source.putAll(map);
        return source;
    }

    /**
     * 判断Map中的值是否为空
     *
     * <pre>
     * MapUtils.valuesIsNull({1=1,2=null,3=null},2,3) = true;
     * MapUtils.valuesIsNull({1=1,2=null,3=null},1,3) = false;
     * </pre>
     *
     * @param map
     * @param keys 需要判断的Keys
     * @return 是否为空
     */
    public static <K, V> boolean valuesIsNull(Map<K, V> map, @SuppressWarnings("unchecked") K... keys) {
        for (K key : keys) {
            if (map.get(key) != null)
                return false;
        }
        return true;
    }

    /**
     * 判断Map中的值是否不为空
     *
     * <pre>
     * MapUtils.valuesIsNotNull({1=1,2=null,3=null},2,3) = false;
     * MapUtils.valuesIsNotNull({1=1,2=null,3=null},1,3) = false;
     * MapUtils.valuesIsNotNull({1=1,2=null,3=null},1)   = true;
     * </pre>
     *
     * @param map
     * @param keys 需要判断的Keys
     * @return 是否为空
     */
    public static <K, V> boolean valuesIsNotNull(Map<K, V> map, @SuppressWarnings("unchecked") K... keys) {
        for (K key : keys) {
            if (map.get(key) == null)
                return false;
        }
        return true;
    }

    /**
     * 取出Map中的一些值
     *
     * <pre>
     * MapUtils.getValues({1=1,2=null,3=null},1,2) = 1,null;
     * MapUtils.getValues({1=1,2=null,3=null},2,3) = null,null;
     * </pre>
     *
     * @param map
     * @param keys
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> List<V> getValues(Map<K, V> map, K... keys) {
        ArrayList<V> values = new ArrayList<V>();
        for (K key : keys) {
            values.add((V) MapUtils.getObject(map, key));
        }
        return values;
    }

    /**
     * 从Map中移除一些Key
     *
     * @param map
     * @param keys 需要移除的Key
     */
    public static <K, V> void remove(Map<K, V> map, @SuppressWarnings("unchecked") K... keys) {
        if (MapUtils.isEmpty(map))
            return;
        for (K k : keys) {
            map.remove(k);
        }
    }

    /**
     * 从Map中获取List对象
     *
     * @param map
     * @param key
     * @return
     */
    public static <K, V extends List<?>> V getList(final Map<K, V> map, final Object key) {
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    /**
     * 从Map中获取Date对象
     *
     * @param map
     * @param key
     * @return
     * @throws ParseException
     */
    public static <K, V> Date getDate(final Map<K, V> map, final Object key) {
        if (map != null) {
            Object answer = map.get(key);
            if (answer != null) {
                return TimeUtils.trueTime(answer);
            }
        }
        return null;

    }

    /**
     * 添加对象的时候，如果存在K则会自动生成一个List
     *
     * <pre>
     * MapUtils.safeAddToMapList(map, "k1", "v1")
     * MapUtils.safeAddToMapList(map, "k2", "v2")
     * MapUtils.safeAddToMapList(map, "k1", "v3")
     * return {k1=[v1, v3], k2=[v2]}
     * </pre>
     *
     * @param map
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, E, V extends List<E>> void safeAddToMapList(final Map<K, V> map, K key, E value) {
        if (map == null)
            return;
        Collection<E> list = map.get(key);
        if (list == null) {
            list = new ArrayList<E>();
            map.put(key, (V) list);
        }
        list.add(value);
    }

    /**
     * 阅后即焚
     *
     * @param map
     * @param key
     * @param value
     * @return
     */
    public static <K, V> V readAndRemove(final Map<K, V> map, K key, V value) {
        synchronized (map) {
            final V ret = map.get(key);
            map.remove(key);
            if (ret == null)
                return value;
            return ret;
        }
    }

    /**
     * MapUtils.fromArray(new Object[] { "1231", 123, "123123", 1231 }) =
     * {123123=1231, 1231=123}
     *
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> fromArray(Object[] array) {
        final Map<K, V> map = Maps.newHashMap();
        for (int i = 0; i < array.length; i++) {
            final Object obj = array[i];
            K key = null;
            V value = null;
            if ((i + 1) % 2 == 0) {
                key = (K) array[i - 1];
                value = (V) obj;
            } else {
                key = (K) obj;
            }
            map.put(key, value);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> fromList(Collection<V> values, String keyName, Class<K> keyClass)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<K, V> map = Maps.newHashMap();
        for (V value : values) {
            K key = (K) PropertyUtils.getSimpleProperty(value, keyName);
            map.put(key, value);
        }
        return map;
    }

//    public static void main(String[] args) {
//        String source = "1=1,2=2,3=3   ";
//        String source2 = "1:1;2:2;3:3;  ";
//        Map<String, String> map = MapUtils.split(source, null);
//        Map<String, String> map2 = MapUtils.split(source2);
//        System.out.println(map2);
//        System.out.println(MapUtils.toList(map));
//        System.out.println(MapUtils.fromArray(new Object[]{"1231", 123, "123123", 1231}));
//    }

}
