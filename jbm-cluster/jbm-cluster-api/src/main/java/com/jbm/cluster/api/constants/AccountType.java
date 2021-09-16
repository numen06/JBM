package com.jbm.cluster.api.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;

/***
 * 授权类型
 */
public enum AccountType {

    username("用户名密码"), mobile("手机号"), email("邮箱"), weixin("微信"), weibo("微博"), qq("QQ");

    @EnumValue
    private final String key;
    private final String value;

    AccountType(String value) {
        this.key = this.toString();
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
