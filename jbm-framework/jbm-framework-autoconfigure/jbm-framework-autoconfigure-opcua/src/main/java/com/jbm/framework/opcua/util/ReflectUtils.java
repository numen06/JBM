package com.jbm.framework.opcua.util;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.framework.opcua.annotation.OpcUaHeartBeat;
import com.jbm.framework.opcua.annotation.OpcUaReadField;
import com.jbm.framework.opcua.annotation.OpcUaWriteField;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;

/**
 * @author fanscat
 * @createTime 2022/11/1 19:46
 */
public class ReflectUtils {

    /**
     * 通过反射set方法设置字段值
     *
     * @param obj       对象,static字段则此处传Class
     * @param fieldName 字段名
     * @param value     值，值类型必须与字段类型匹配，不会自动转换对象类型
     */
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        Class<?> beanClass = ClassUtils.getUserClass(obj.getClass());
        Field field = ReflectUtil.getField(beanClass, fieldName);
        Assert.notNull(field, "Field [{}] is not exist in [{}]", fieldName, beanClass.getName());
        setFieldValue(obj, field, value);
    }

    /**
     * 通过反射set方法设置字段值
     *
     * @param obj   对象,static字段则此处传Class
     * @param field 字段
     * @param value 值，值类型必须与字段类型匹配，不会自动转换对象类型
     */
    public static void setFieldValue(Object obj, Field field, Object value) {
        ReflectUtil.invoke(obj, StrUtil.genSetter(field.getName()), value);
    }

    /**
     * 通过OPC UA点位的别名获取Bean当中的字段
     *
     * @param source Bean的实例
     * @param alias  OPC UA的点位别名
     * @return 点位对应的字段
     */
    public static Field getReadField(Object source, String alias) {
        Class<?> beanClass = ClassUtils.getUserClass(source.getClass());
        final Field[] fields = ReflectUtil.getFields(beanClass);
        Field field = ArrayUtil.firstMatch((item) -> (item.isAnnotationPresent(OpcUaHeartBeat.class) && ObjectUtil.equals(alias, item.getAnnotation(OpcUaHeartBeat.class).read()))
                || (item.isAnnotationPresent(OpcUaReadField.class) && ObjectUtil.equals(alias, item.getAnnotation(OpcUaReadField.class).alias())), fields);
        Assert.notNull(field, "Field [{}] is not exist in [{}]", alias, beanClass.getName());
        return field;
    }

    /**
     * 获取字段对应的OPC UA点位别名
     *
     * @param field 字段
     * @return OPC UA的点位别名
     */
    public static String getReadFieldAlias(Field field) {
        return field.isAnnotationPresent(OpcUaHeartBeat.class) ? field.getAnnotation(OpcUaHeartBeat.class).read() :
                field.isAnnotationPresent(OpcUaReadField.class) ? field.getAnnotation(OpcUaReadField.class).alias() : null;
    }

    /**
     * 通过OPC UA点位的别名获取Bean当中的字段
     *
     * @param source Bean的实例
     * @param alias  OPC UA的点位别名
     * @return 点位对应的字段
     */
    public static Field getWriteField(Object source, String alias) {
        Class<?> beanClass = ClassUtils.getUserClass(source.getClass());
        final Field[] fields = ReflectUtil.getFields(beanClass);
        Field field = ArrayUtil.firstMatch((item) -> (item.isAnnotationPresent(OpcUaHeartBeat.class) && ObjectUtil.equals(alias, item.getAnnotation(OpcUaHeartBeat.class).write()))
                || (item.isAnnotationPresent(OpcUaWriteField.class) && ObjectUtil.equals(alias, item.getAnnotation(OpcUaWriteField.class).alias())), fields);
        Assert.notNull(field, "Field [{}] is not exist in [{}]", alias, beanClass.getName());
        return field;
    }

    /**
     * 获取字段对应的OPC UA点位别名
     *
     * @param field 字段
     * @return OPC UA的点位别名
     */
    public static String getWriteFieldAlias(Field field) {
        return field.isAnnotationPresent(OpcUaHeartBeat.class) ? field.getAnnotation(OpcUaHeartBeat.class).write() :
                field.isAnnotationPresent(OpcUaWriteField.class) ? field.getAnnotation(OpcUaWriteField.class).alias() : null;
    }

    /**
     * 获取字段对应的OPC UA点位别名
     *
     * @param source Bean的实例
     * @param name   字段名
     * @return OPC UA的点位别名
     */
    public static String getWriteFieldAlias(Object source, String name) {
        Class<?> beanClass = ClassUtils.getUserClass(source.getClass());
        Field field = ReflectUtil.getField(beanClass, name);
        return field.isAnnotationPresent(OpcUaHeartBeat.class) ? field.getAnnotation(OpcUaHeartBeat.class).write() :
                field.isAnnotationPresent(OpcUaWriteField.class) ? field.getAnnotation(OpcUaWriteField.class).alias() : null;
    }
}
