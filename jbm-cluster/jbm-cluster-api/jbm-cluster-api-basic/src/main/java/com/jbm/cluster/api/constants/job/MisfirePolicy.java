package com.jbm.cluster.api.constants.job;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

@JbmDicType(typeName = "任务执行类型")
public enum MisfirePolicy {

    DEFAULT(0, "默认"), IGNORE_MISFIRES(1, "立即触发执行"),
    FIRE_AND_PROCEED(2, "触发一次执行"), DO_NOTHING(3, "不触发立即执行");

    @EnumValue
    private final int key;
    private final String value;

    MisfirePolicy(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
