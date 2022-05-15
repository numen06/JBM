package com.jbm.framework.exceptions;

/**
 * 验证码错误异常类
 *
 * @author wesley.zhang
 */
public class CaptchaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CaptchaException() {
    }

    public CaptchaException(String msg) {
        super(msg);
    }
}
