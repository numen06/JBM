package com.jbm.autoconfig.dic;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.dictionary.JbmDictionary;
import com.jbm.framework.dictionary.annotation.JbmDicCode;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import com.jbm.framework.dictionary.annotation.JbmDicValue;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class EnumTypeConverter implements ITypeConverter<Class<? extends Enum<?>>> {


    /**
     * 获取CODE的字段
     */
    private final static String CODE_FIELD = "code";

    /**
     * 获取值的字段
     */
    private final static String VALUE_FIELD = "value";

    /**
     * 获取值的字段
     */
    private final static String TYPE_FIELD = "type";


    private final static String TYPE_NAME_FIELD = "typeName";

    @Override
    public List<JbmDictionary> convert(Class<? extends Enum<?>> emClass) {
        final Enum<?>[] enums = emClass.getEnumConstants();
        final List<JbmDictionary> jbmDictionaries = Lists.newArrayList();
        // 只获取当前枚举类自己声明的字段，不包括 Enum 父类的私有字段
        final List<String> fields = getEnumDeclaredFieldNames(emClass);
        final Map<String, String> keys = this.parseCodeAnnotation(emClass, fields);
        final EnumType enumType = getType(emClass);
//        if (StrUtil.isBlank(type)) {
//            throw new NullPointerException("字典的类型不能为空");
//        }
        for (Enum<?> e : enums) {
            //不存在value
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(TYPE_FIELD, enumType.getType());
            jsonObject.put(TYPE_NAME_FIELD, enumType.getTypeName());
            JbmDictionary jbmDictionary = this.putValue(jsonObject, keys, e, fields);
            if (jbmDictionary == null) {
                return jbmDictionaries;
            }
            //设置类型
            putCodeValue(jsonObject, keys, e);
            for (String field : fields) {
                if (CollectionUtil.contains(keys.values(), field)) {
                    continue;
                }
                // 已经只获取枚举子类声明的字段，不会包含 Enum 的内置字段
                jsonObject.put(field, ReflectUtil.getFieldValue(e, field));
            }
            jbmDictionary = jsonObject.toJavaObject(JbmDictionary.class);
//            jbmDictionary.setValues(jsonObject);
            jbmDictionaries.add(jbmDictionary);
        }
        return jbmDictionaries;
    }

    private EnumType getType(Class<? extends Enum<?>> emClass) {
        JbmDicType jbmDicType = emClass.getDeclaredAnnotation(JbmDicType.class);
        EnumType enumType = new EnumType();
        if (ObjectUtil.isNotEmpty(jbmDicType)) {
            String type = jbmDicType.value();
            enumType.setType(StrUtil.isBlank(type) ? emClass.getSimpleName() : type);
            enumType.setTypeName(StrUtil.isBlank(jbmDicType.typeName()) ? emClass.getSimpleName() : jbmDicType.typeName());
        } else {
            enumType.setType(emClass.getSimpleName());
            enumType.setTypeName(emClass.getSimpleName());
        }
        return enumType;
    }

    private JbmDictionary putValue(JSONObject jsonObject, Map<String, String> keys, Enum<?> e, List<String> fields) {
        JbmDictionary jbmDictionary = new JbmDictionary();
        final String key = keys.containsKey(VALUE_FIELD) ? keys.get(VALUE_FIELD) : VALUE_FIELD;
        if (!CollectionUtil.contains(fields, key)) {
            return null;
        }
        jsonObject.put(VALUE_FIELD, ReflectUtil.getFieldValue(e, key));
        jbmDictionary.setValue(jsonObject.getString(VALUE_FIELD));
//        fields.remove(key);
        return jbmDictionary;
    }

    private void putCodeValue(JSONObject jsonObject, Map<String, String> keys, Enum<?> e) {
        final String key = keys.get(CODE_FIELD);
        if (ObjectUtil.isNotEmpty(key)) {
            jsonObject.put("code", ReflectUtil.getFieldValue(e, keys.get(CODE_FIELD)));
        } else {
            jsonObject.put("code", e.toString());
        }
    }

    public Map<String, String> parseCodeAnnotation(Class<? extends Enum<?>> emClass, List<String> fields) {
        Map<String, String> maps = Maps.newHashMap();
        maps.put("value", "value");
        for (String fieldName : fields) {
            // 跳过 Enum 内置字段
            if ("name".equals(fieldName) || "ordinal".equals(fieldName)) {
                continue;
            }
            
            Field field = ReflectUtil.getField(emClass, fieldName);
            if (field == null) {
                continue;
            }
            
            // 尝试加载 MyBatis Plus 的 @EnumValue 注解(如果存在)
            try {
                Class clz = ClassUtil.loadClass("com.baomidou.mybatisplus.annotation.EnumValue");
                if (ObjectUtil.isNotEmpty(clz)) {
                    if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(clz))) {
                        maps.put(CODE_FIELD, fieldName);
                    }
                }
            } catch (Exception e) {
                // MyBatis Plus 不存在时忽略，不影响 JBM 自己的注解处理
            }
            
            // 检查 JBM 字典注解
            if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(JbmDicCode.class))) {
                maps.put(CODE_FIELD, fieldName);
            }
            if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(JbmDicValue.class))) {
                maps.put(VALUE_FIELD, fieldName);
            }
        }
        return maps;
    }

    /**
     * 获取枚举类自己声明的字段名称列表
     * 不包括 Enum 父类的字段（name, ordinal, hash 等）
     */
    private List<String> getEnumDeclaredFieldNames(Class<? extends Enum<?>> emClass) {
        List<String> fieldNames = Lists.newArrayList();
        // 只获取当前类声明的字段，不包括父类字段
        Field[] declaredFields = emClass.getDeclaredFields();
        for (Field field : declaredFields) {
            // 跳过枚举常量本身（类型是枚举类型的字段）和合成字段
            if (!field.isSynthetic() && !field.isEnumConstant()) {
                fieldNames.add(field.getName());
            }
        }
        return fieldNames;
    }

    @Data
    class EnumType {
        private String type;
        private String typeName;
    }

}
