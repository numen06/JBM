package com.jbm.framework.masterdata.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2020-03-16 01:41
 **/
@JbmDicType(typeName = "已读状态")
public enum ReadStatus {

    unread(0, "未读"), read(1, "已读");

    @EnumValue
    private final int key;
    private final String value;

    ReadStatus(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public boolean isRead() {
        return key == 1;
    }

    public int getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
