package com.jbm.cluster.api.constants.job;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

@JbmDicType(typeName = "任务执行状态")
public enum ScheduleStauts {

    NORMAL(0, "正常"), PAUSE(1, "暂停");

    @EnumValue
    private final int key;
    private final String value;

    ScheduleStauts(int key, String value) {
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
