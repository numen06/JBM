package com.jbm.framework.exceptions.auth;

/**
 * 未能通过的登录认证异常
 *
 * @author wesley.zhang
 */
public class NotLoginException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String type;

    private String loginType;

    /**
     * 表示未提供token
     */
    public static final String NOT_TOKEN = "-1";
    public static final String NOT_TOKEN_MESSAGE = "未能读取到有效Token";

    /**
     * 表示token无效
     */
    public static final String INVALID_TOKEN = "-2";
    public static final String INVALID_TOKEN_MESSAGE = "Token无效";

    /**
     * 表示token已过期
     */
    public static final String TOKEN_TIMEOUT = "-3";
    public static final String TOKEN_TIMEOUT_MESSAGE = "Token已过期";

    /**
     * 表示token已被顶下线
     */
    public static final String BE_REPLACED = "-4";
    public static final String BE_REPLACED_MESSAGE = "Token已被顶下线";

    /**
     * 表示token已被踢下线
     */
    public static final String KICK_OUT = "-5";
    public static final String KICK_OUT_MESSAGE = "Token已被踢下线";

    public static final String DEFAULT_MESSAGE = "登录已过期，请重新登录";

    public NotLoginException(String message, String loginType, String type) {
        super(message);
        this.loginType = loginType;
        this.type = type;
    }

    public static NotLoginException newInstance(String loginType, String type) {
        return new NotLoginException(NotLoginException.DEFAULT_MESSAGE, loginType, type);
    }
}
