package com.jbm.cluster.api.constants.center;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import lombok.Getter;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 11:03
 */
@Getter
@JbmDicType(typeName = "表单或表格" , value = "form_table")
public enum FormOrTable {
    form("form", "表单"),
    table("table", "表格");
    @EnumValue
    private final String key;
    private final String value;

    FormOrTable(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
