package com.jbm.cluster.api.constants.center;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import lombok.Getter;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 11:46
 */
@Getter
@JbmDicType(typeName = "字段类型", value = "field_type")
public enum FieldType {
    text("text", "文本"), number("number", "数字"), date("date", "日期"), radio("radio", "单选框"), checkbox("checkbox", "复选框");
    @EnumValue
    private final String key;
    private final String value;

    FieldType(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
