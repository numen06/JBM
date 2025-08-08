package com.jbm.cluster.api.constants.center;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import lombok.Getter;

/**
 * @author scolin
 * @description
 * @date 2025/8/6 10:13
 */
@Getter
@JbmDicType(typeName = "组件类型", value = "component_type")
public enum ComponentType {
    input("input","输入框"),
    textarea("textarea","多行文本框"),
    select("select","下拉选择框"),
    inputNumber("inputNumber","数字输入框"),
    datePicker("datePicker","日期选择框"),
    switchPicker("switchPicker","开关"),
    radio("radio","单选组"),
    checkbox("checkbox","多选组"),
    cascader("cascader","级联"),
    slot("slot","自定义");

    @EnumValue
    private final String key;
    private final String value;
    ComponentType(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
