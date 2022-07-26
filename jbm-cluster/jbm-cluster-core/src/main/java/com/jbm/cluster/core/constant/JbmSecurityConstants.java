package com.jbm.cluster.core.constant;

/**
 * 权限相关通用常量
 *
 * @author wesley.zhang
 */
public class JbmSecurityConstants {
    /**
     * 用户ID字段
     */
    public static final String DETAILS_USER_ID = "user_id";

    /**
     * 用户名字段
     */
    public static final String DETAILS_USERNAME = "username";

    /**
     * 授权信息字段
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 请求来源
     */
    public static final String FROM_SOURCE = "from-source";

    /**
     * 内部请求
     */
    public static final String INNER = "inner";

    /**
     * 用户标识
     */
    public static final String USER_KEY = "user_key";

    /**
     * 登录用户
     */
    public static final String LOGIN_USER = "login_user";


    public final static String OPEN_ID = "openid";
    public final static String DOMAIN = "domain";
    public final static String AUTHORITY_PREFIX_MENU = "MENU_";
    public final static String AUTHORITY_PREFIX_ROLE = "ROLE_";
    public final static String AUTHORITY_PREFIX_API = "API_";
    public final static String AUTHORITY_PREFIX_ACTION = "ACTION_";

    public final static String LOGIN_AUTHENTICATE_KEY = "LoginAuthenticate:";
}
