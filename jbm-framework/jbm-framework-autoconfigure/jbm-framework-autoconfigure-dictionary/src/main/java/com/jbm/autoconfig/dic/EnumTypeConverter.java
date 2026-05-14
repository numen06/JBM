package com.jbm.autoconfig.dic;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.*;
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

    @Override
    public List<JbmDictionary> convert(Class<? extends Enum<?>> emClass) {
        final Enum<?>[] enums = emClass.getEnumConstants();
        final List<JbmDictionary> jbmDictionaries = Lists.newArrayList();
        final List<String> fields = EnumUtil.getFieldNames(emClass);
        final Map<String, String> keys = this.parseCodeAnnotation(emClass, fields);
        final EnumType enumType = getType(emClass);
        for (Enum<?> e : enums) {
            String displayValue = resolveDisplayValue(keys, e, fields);
            if (displayValue == null) {
                return jbmDictionaries;
            }
            JbmDictionary jbmDictionary = new JbmDictionary();
            jbmDictionary.setType(enumType.getType());
            jbmDictionary.setTypeName(enumType.getTypeName());
            jbmDictionary.setCode(resolveCode(keys, e));
            jbmDictionary.setValue(displayValue);
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

    /**
     * 字典展示名：来自 @JbmDicValue 指向的字段，或默认名为 {@code value} 的字段。
     */
    private String resolveDisplayValue(Map<String, String> keys, Enum<?> e, List<String> fields) {
        final String key = keys.containsKey(VALUE_FIELD) ? keys.get(VALUE_FIELD) : VALUE_FIELD;
        if (!CollectionUtil.contains(fields, key)) {
            return null;
        }
        return toDictionaryString(ReflectUtil.getFieldValue(e, key));
    }

    /**
     * 字典编码：来自 @EnumValue / @JbmDicCode 指向的字段；未标注时用枚举常量名。
     */
    private String resolveCode(Map<String, String> keys, Enum<?> e) {
        final String key = keys.get(CODE_FIELD);
        if (ObjectUtil.isNotEmpty(key)) {
            return toDictionaryString(ReflectUtil.getFieldValue(e, key));
        }
        return e.toString();
    }

    private static String toDictionaryString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public Map<String, String> parseCodeAnnotation(Class<? extends Enum<?>> emClass, List<String> fields) {
        Map<String, String> maps = Maps.newHashMap();
        maps.put("value", "value");
        for (String fieldName : fields) {
            Field field = ReflectUtil.getField(emClass, fieldName);
            if (field == null) {
                continue;
            }
            Class clz = ClassUtil.loadClass("com.baomidou.mybatisplus.annotation.EnumValue");
            if (ObjectUtil.isNotEmpty(clz)) {
                if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(clz))) {
                    maps.put(CODE_FIELD, fieldName);
                }
            }
            if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(JbmDicCode.class))) {
                maps.put(CODE_FIELD, fieldName);
            }
            if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(JbmDicValue.class))) {
                maps.put(VALUE_FIELD, fieldName);
            }
        }
        return maps;
    }

    @Data
    class EnumType {
        private String type;
        private String typeName;
    }

}
