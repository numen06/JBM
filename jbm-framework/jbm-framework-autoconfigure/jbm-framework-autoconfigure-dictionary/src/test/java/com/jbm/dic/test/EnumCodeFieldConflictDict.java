package com.jbm.dic.test;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/**
 * 复现：@EnumValue 在 key 上，另有名为 code 的字段；旧实现用字段名作为 JSON 键会覆盖字典 code。
 */
@JbmDicType(typeName = "code 字段名冲突")
public enum EnumCodeFieldConflictDict {

    ONE("stored-key", "biz-code", "展示一");

    @EnumValue
    private final String key;
    /** 与 JbmDictionary 属性 code 同名，旧实现会覆盖 JSON 键 */
    private final String code;
    private final String value;

    EnumCodeFieldConflictDict(String key, String code, String value) {
        this.key = key;
        this.code = code;
        this.value = value;
    }
}
