package com.jbm.framework.opcua.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.*;
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
     * 查找指定类中的指定name的字段（包括非public字段），也包括父类和Object类的字段， 字段不存在则返回{@code null}
     *
     * @param obj  对象
     * @param name 字段名
     * @return 字段
     * @throws SecurityException 安全异常
     */
    public static Field getField(Object obj, String name) throws SecurityException {
        return ReflectUtil.getField(ClassUtils.getUserClass(obj.getClass()), name);
    }

    /**
     * 通过Getter方法获取字段值
     *
     * @param obj       对象
     * @param fieldName 字段名
     * @return 字段值
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        Class<?> beanClass = ClassUtils.getUserClass(obj.getClass());
        Field field = ReflectUtil.getField(beanClass, fieldName);
        Assert.notNull(field, "Field [{}] is not exist in [{}]", fieldName, beanClass.getName());
        return getFieldValue(obj, field);
    }

    /**
     * 通过Getter方法获取字段值
     *
     * @param obj   对象
     * @param field 字段
     * @return 字段值
     */
    public static Object getFieldValue(Object obj, Field field) {
        return ReflectUtil.invoke(obj, StrUtil.genGetter(field.getName()));
    }

    /**
     * 通过Setter方法设置字段值<br>
     * 若值类型与字段类型不一致，则会尝试通过 {@link Convert} 进行转换<br>
     * 若字段类型是原始类型而传入的值是 null，则会将字段设置为对应原始类型的默认值（见 {@link ClassUtil#getDefaultValue(Class)}）
     *
     * @param obj       对象,static字段则此处传Class
     * @param fieldName 字段名
     * @param value     值，当值类型与字段类型不匹配时，会尝试转换
     */
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        Class<?> beanClass = ClassUtils.getUserClass(obj.getClass());
        Field field = ReflectUtil.getField(beanClass, fieldName);
        Assert.notNull(field, "Field [{}] is not exist in [{}]", fieldName, beanClass.getName());
        setFieldValue(obj, field, value);
    }

    /**
     * 通过Setter方法设置字段值<br>
     * 若值类型与字段类型不一致，则会尝试通过 {@link Convert} 进行转换<br>
     * 若字段类型是原始类型而传入的值是 null，则会将字段设置为对应原始类型的默认值（见 {@link ClassUtil#getDefaultValue(Class)}）<br>
     *
     * @param obj   对象，如果是static字段，此参数为null
     * @param field 字段
     * @param value 值，当值类型与字段类型不匹配时，会尝试转换
     */
    public static void setFieldValue(Object obj, Field field, Object value) {
        final Class<?> fieldType = field.getType();
        if (null != value) {
            if (false == fieldType.isAssignableFrom(value.getClass())) {
                //对于类型不同的字段，尝试转换，转换失败则使用原对象类型
                final Object targetValue = Convert.convert(fieldType, value);
                if (null != targetValue) {
                    value = targetValue;
                }
            }
        } else {
            // 获取null对应默认值，防止原始类型造成空指针问题
            value = ClassUtil.getDefaultValue(fieldType);
        }
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
    public static String getReadAlias(Field field) {
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
    public static String getWriteAlias(Field field) {
        return field.isAnnotationPresent(OpcUaHeartBeat.class) ? field.getAnnotation(OpcUaHeartBeat.class).write() :
                field.isAnnotationPresent(OpcUaWriteField.class) ? field.getAnnotation(OpcUaWriteField.class).alias() : null;
    }
}
