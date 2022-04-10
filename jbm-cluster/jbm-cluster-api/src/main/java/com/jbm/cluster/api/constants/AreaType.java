package com.jbm.cluster.api.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

@JbmDicType(typeName = "区域类型")
public enum AreaType {

    country(1, "国家"), province(2, "省"), city(3, "市"), district(4, "区");

    @EnumValue
    private final int key;
    private final String value;

    AreaType(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getKey() {
        return key;
    }
}
