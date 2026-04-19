package com.jbm.dic.test;

import com.jbm.framework.dictionary.annotation.JbmDicCode;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import com.jbm.framework.dictionary.annotation.JbmDicValue;

/**
 * 复现：@JbmDicValue 在 name 上，另有名为 value 的字段；旧实现会覆盖字典 value。
 */
@JbmDicType(typeName = "value 字段名冲突")
public enum EnumValueFieldConflictDict {

    ONE("c1", "展示", "wrong");

    @JbmDicCode
    private final String code;
    @JbmDicValue
    private final String name;
    /** 与 JbmDictionary 属性 value 同名，旧实现会覆盖 JSON 键 */
    private final String value;

    EnumValueFieldConflictDict(String code, String name, String value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }
}
