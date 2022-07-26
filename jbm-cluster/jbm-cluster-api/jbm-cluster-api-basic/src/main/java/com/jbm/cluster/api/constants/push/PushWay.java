package com.jbm.cluster.api.constants.push;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.jbm.framework.dictionary.annotation.JbmDicType;

/***
 * 授权类型
 */
@JbmDicType(typeName = "通知方式")
public enum PushWay {

    internal("站内通知"), mqtt("监听推送"), wechat("微信公众号"), miniapp("微信小程序"), email("邮箱"), sms("短信"), app("应用推送");

    @EnumValue
    private final String key;
    private final String value;

    PushWay(String value) {
        this.key = this.toString();
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
