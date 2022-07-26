package com.jbm.cluster.api.constants.push;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/***
 * 授权类型
 */
@JbmDicType(typeName = "推送消息类型")
public enum PushMsgType {

    notification("通知"), alarm("警报"), alert("弹窗");

    @EnumValue
    private final String key;
    private final String value;

    PushMsgType(String value) {
        this.key = this.toString();
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
