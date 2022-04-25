package com.jbm.cluster.api.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/***
 * 授权类型
 */
@JbmDicType(typeName = "组织架构类型")
public enum OrgType {

    company("公司"), department("部门");

    @EnumValue
    private final String key;
    private final String value;

    OrgType(String value) {
        this.key = this.toString();
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
