package com.jbm.dic.test;

import com.jbm.framework.dictionary.annotation.JbmDicCode;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import com.jbm.framework.dictionary.annotation.JbmDicValue;

/**
 * 无 MyBatis @EnumValue，仅用 @JbmDicCode / @JbmDicValue，编码与名称均为中文。
 */
@JbmDicType(typeName = "中文注解字典")
public enum EnumChineseJbmDicDict {

    A("业务码甲", "名称甲");

    @JbmDicCode
    private final String code;
    @JbmDicValue
    private final String name;

    EnumChineseJbmDicDict(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
