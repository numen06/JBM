package com.jbm.cluster.core.constant;

/**
 * 缓存的key 常量
 *
 * @author wesley.zhang
 */
public class JbmCacheConstants {
    /**
     * 缓存有效期，默认720（分钟）
     */
    public final static long EXPIRATION = 720;

    /**
     * 缓存刷新时间，默认120（分钟）
     */
    public final static long REFRESH_TIME = 120;

//    /**
//     * 权限缓存前缀
//     */
//    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * loginid构造拼接字符串
     */
    public final static String LOGINID_JOIN_CODE = ":";

    /**
     * 登录用户 redis key
     */
    public final static String LOGIN_TOKEN_KEY = "Authorization:login:token:";

    /**
     * 在线用户 redis key
     */
    public final static String ONLINE_TOKEN_KEY = "online_tokens:";


    /**
     * 登陆错误 redis key
     */
    public final static String LOGIN_ERROR = "login_error:";

    /**
     * 登录错误次数
     */
    public final static Integer LOGIN_ERROR_NUMBER = 5;

    /**
     * 登录错误限制时间(分钟)
     */
    public final static Integer LOGIN_ERROR_LIMIT_TIME = 10;

}
