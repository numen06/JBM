package com.jbm.cluster.auth.form;

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
     * 图形验证码
     */
    private String vcode;
    /**
     * 应用标识，phone 为 RSA 密文时必填
     */
    private String appKey;
}
