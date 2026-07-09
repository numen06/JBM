package com.jbm.cluster.auth.form;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * 手机验证码请求（POST body，避免敏感信息出现在 URL）
 */
@Data
public class PhoneCaptchaForm {
    /**
     * 手机号（明文或 RSA 密文）
     */
    private String phone;
    /**
     * 图形验证码（仅发码 POST /captcha/pcode 使用）
     */
    private String vcode;
    /**
     * 短信验证码（验码 POST /captcha/pcode/verify 使用，6 位）
     */
    private String pcode;
    /**
     * 应用标识，phone 为 RSA 密文时必填
     */
    private String appKey;

    /**
     * 解析短信验证码：优先 pcode，兼容 GET 迁移时 vcode 传短信码
     */
    public String resolveSmsCode() {
        if (StrUtil.isNotBlank(pcode)) {
            return StrUtil.trim(pcode);
        }
        if (StrUtil.isNotBlank(vcode)) {
            return StrUtil.trim(vcode);
        }
        return null;
    }
}
