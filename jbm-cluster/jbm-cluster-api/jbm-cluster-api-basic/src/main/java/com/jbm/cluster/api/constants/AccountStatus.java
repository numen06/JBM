package com.jbm.cluster.api.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

@JbmDicType(typeName = "账号状态")
public enum AccountStatus {

    DISABLE(0, "禁用"), NORMAL(1, "正常"), LOCKED(2, "锁定");

    @EnumValue
    private final Integer key;
    private final String value;

    AccountStatus(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    public Integer getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
