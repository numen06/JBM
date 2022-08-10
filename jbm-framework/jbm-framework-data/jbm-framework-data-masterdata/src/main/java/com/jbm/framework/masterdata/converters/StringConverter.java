package com.jbm.framework.masterdata.converters;

import com.alibaba.fastjson.JSON;

import javax.persistence.AttributeConverter;

public class StringConverter implements AttributeConverter<Object, String> {
    @Override
    public String convertToDatabaseColumn(Object o) {
        return JSON.toJSONString(o);
    }

    @Override
    public Object convertToEntityAttribute(String s) {
        return JSON.parseObject(s);
    }

}
