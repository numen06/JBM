package com.jbm.cluster.platform.gateway.service;

import com.jbm.framework.exceptions.CaptchaException;
import com.jbm.framework.metadata.bean.ResultBody;

import java.io.IOException;
import java.util.Map;

/**
 * 验证码处理
 *
 * @author wesley.zhang
 */
public interface ValidateCodeService {
    /**
     * 生成验证码
     */
    ResultBody<Map<String, Object>> createCaptcha() throws IOException, CaptchaException;

    /**
     * 校验验证码
     */
    void checkCaptcha(String key, String value) throws CaptchaException;

    /**
     * 校验 Auth 侧图形验证码（vcode 字段，与 VCoderService 存储一致）
     */
    void verifyVcode(String vcode) throws CaptchaException;
}
