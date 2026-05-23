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

    public final static String APP_CACHE_NAMESPACE = "apps";

    public final static String API_KEY_CACHE_NAMESPACE = "apiKeys";

    /**
     * 用户权限标识缓存（与 Session 解耦，便于授权变更后失效）
     */
    public static final String USER_AUTHORITY_KEY = "jbm:user:authority:";

    /**
     * 用户权限缓存默认 TTL（秒），与 sa-token.timeout 对齐时可由配置覆盖
     */
    public static final long USER_AUTHORITY_EXPIRE_SECONDS = 86400L;

}
