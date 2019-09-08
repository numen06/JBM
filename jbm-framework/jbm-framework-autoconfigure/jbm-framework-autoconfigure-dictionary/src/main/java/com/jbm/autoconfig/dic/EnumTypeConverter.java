package com.jbm.autoconfig.dic;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
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

    @Data
    class EnumType {
        private String type;
        private String typeName;
    }


    @Override
    public List<JbmDictionary> convert(Class<? extends Enum<?>> emClass) {
        final Enum<?>[] enums = emClass.getEnumConstants();
        final List<JbmDictionary> jbmDictionaries = Lists.newArrayList();
        final List<String> fields = EnumUtil.getFieldNames(emClass);
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
            if (jbmDictionary == null)
                return jbmDictionaries;
            //设置类型
            putCodeValue(jsonObject, keys, e);
            for (String field : fields) {
                if (CollectionUtil.contains(keys.values(), field))
                    continue;
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
        if (!CollectionUtil.contains(fields, key))
            return null;
        jsonObject.put(VALUE_FIELD, ReflectUtil.getFieldValue(e, key));
        jbmDictionary.setValue(jsonObject.getString(VALUE_FIELD));
//        fields.remove(key);
        return jbmDictionary;
    }

    private void putCodeValue(JSONObject jsonObject, Map<String, String> keys, Enum<?> e) {
        final String key = keys.get(CODE_FIELD);
        if (ObjectUtil.isNotEmpty(key))
            jsonObject.put("code", ReflectUtil.getFieldValue(e, keys.get(CODE_FIELD)));
        else
            jsonObject.put("code", e.toString());
    }


    public Map<String, String> parseCodeAnnotation(Class<? extends Enum<?>> emClass, List<String> fields) {
        Map<String, String> maps = Maps.newHashMap();
        maps.put("value", "value");
        for (String fieldName : fields) {
            Field field = ReflectUtil.getField(emClass, fieldName);
            if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(JbmDicCode.class))) {
                maps.put(CODE_FIELD, fieldName);
            }
            if (ObjectUtil.isNotEmpty(field.getDeclaredAnnotation(JbmDicValue.class))) {
                maps.put(VALUE_FIELD, fieldName);
            }
        }
        return maps;
    }

}
