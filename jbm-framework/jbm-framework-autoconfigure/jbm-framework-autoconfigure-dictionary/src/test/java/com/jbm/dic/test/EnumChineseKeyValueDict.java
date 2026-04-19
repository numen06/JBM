package com.jbm.dic.test;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/**
 * 存储编码与展示名均为中文（@EnumValue 在 key，展示在 value）。
 */
@JbmDicType(typeName = "中文键值")
public enum EnumChineseKeyValueDict {

    ITEM("存储编码壹", "展示标签壹");

    @EnumValue
    private final String key;
    private final String value;

    EnumChineseKeyValueDict(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
