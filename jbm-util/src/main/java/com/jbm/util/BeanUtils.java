package com.jbm.util;

import cn.hutool.core.util.ClassUtil;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * 扩展org.apache.commons.beanutils.BeanUtils<br>
 *
 * @author Wesley<br>
 */
public class BeanUtils extends org.apache.commons.beanutils.BeanUtils {

    static {
        ConvertUtils.registerDate(true);
    }

    /**
     * 将源对象中的值覆盖到目标对象中，仅覆盖源对象中不为NULL值的属性
     *
     * @param dest 目标对象，标准的JavaBean
     * @param orig 源对象，可为Map、标准的JavaBean
     * @throws .
     */
    @SuppressWarnings("rawtypes")
    public static void applyIf(Object dest, Object orig) throws Exception {
        try {
            if (orig instanceof Map) {
                Iterator names = ((Map) orig).keySet().iterator();
                while (names.hasNext()) {
                    String name = (String) names.next();
                    if (PropertyUtils.isWriteable(dest, name)) {
                        Object value = ((Map) orig).get(name);
                        if (value != null) {
                            PropertyUtils.setSimpleProperty(dest, name, value);
                        }
                    }
                }
            } else {
                java.lang.reflect.Field[] fields = orig.getClass().getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    String name = fields[i].getName();
                    if (PropertyUtils.isReadable(orig, name) && PropertyUtils.isWriteable(dest, name)) {
                        Object value = PropertyUtils.getSimpleProperty(orig, name);
                        if (value != null) {
                            PropertyUtils.setSimpleProperty(dest, name, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("将源对象中的值覆盖到目标对象中，仅覆盖源对象中不为NULL值的属性", e);
        }
    }

    /**
     * 将源对象中的值覆盖到目标对象中，仅覆盖源对象中不为NULL值的属性
     *
     * @param orig 源对象，标准的JavaBean
     * @param dest 排除检查的属性，Map
     * @throws .
     */
    @SuppressWarnings("rawtypes")
    public static boolean checkObjProperty(Object orig, Map dest) throws Exception {
        try {
            java.lang.reflect.Field[] fields = orig.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                if (!dest.containsKey(name)) {
                    if (PropertyUtils.isReadable(orig, name)) {
                        Object value = PropertyUtils.getSimpleProperty(orig, name);
                        if (value == null) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new Exception("将源对象中的值覆盖到目标对象中，仅覆盖源对象中不为NULL值的属性", e);
        }
    }

    /**
     * 克隆一个Bean
     *
     * @param bean
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static <T> T cloneJavaBean(T bean) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        return (T) cloneBean(bean);
    }

    /**
     * Check if the given type represents a "simple" value type: a primitive, a
     * String or other CharSequence, a Number, a Date, a URI, a URL, a Locale or
     * a Class.
     *
     * @param clazz the type to check
     * @return whether the given type represents a "simple" value type
     */
    public static boolean isSimpleValueType(Class<?> clazz) {
        return ClassUtil.isSimpleValueType(clazz);
    }

    public static <K, V> Map<K, V> createFastCache() {
        return BeanUtils.createCache();
    }

    /**
     * 获取为空的字段
     *
     * @param source
     * @return
     */
    public static String[] getNullPropertyNames(Object source) {
        PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(source);
        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue;
            try {
                srcValue = PropertyUtils.getProperty(source, pd.getName());
            } catch (Exception e) {
                continue;
            }
            if (srcValue == null)
                emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    /**
     * 获取到不为空的字段
     *
     * @param source
     * @return
     */
    public static String[] getNotNullPropertyNames(Object source) {
        return getNotNullPropertyNames(source, "class");
    }

    public static String[] getNotNullPropertyNames(Object source, String... ignores) {
        PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(source);
        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue;
            try {
                if (ArrayUtils.contains(ignores, pd.getName()))
                    continue;
                srcValue = PropertyUtils.getProperty(source, pd.getName());
            } catch (Exception e) {
                continue;
            }
            if (srcValue == null) {
                // emptyNames.add(pd.getName());
            } else {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
}
