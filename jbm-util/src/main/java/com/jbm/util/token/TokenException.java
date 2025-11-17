package com.jbm.util.token;

/**
 * 表示 Token 操作（获取、刷新）过程中发生的错误。
 */
public class TokenException extends Exception {
    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}