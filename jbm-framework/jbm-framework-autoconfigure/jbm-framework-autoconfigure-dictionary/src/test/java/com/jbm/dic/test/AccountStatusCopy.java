package com.jbm.dic.test;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/**
 * 与 {@code com.jbm.cluster.api.constants.AccountStatus} 结构一致，便于本模块单测不依赖 api-basic。
 */
@JbmDicType(typeName = "账号状态")
public enum AccountStatusCopy {

    DISABLE(0, "禁用"), NORMAL(1, "正常"), LOCKED(2, "锁定");

    @EnumValue
    private final Integer key;
    private final String value;

    AccountStatusCopy(Integer key, String value) {
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
