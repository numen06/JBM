package com.jbm.cluster.api.constants.push;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 03:09
 **/
public enum MessageSendStatus {
        unsent("未发送"), wait("等待中"),
        issued("已发送"), fail("发送失败");

    @EnumValue
    private final String key;
    private final String value;

    MessageSendStatus(String value) {
        this.key = this.toString();
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
