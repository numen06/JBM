package com.jbm.cluster.core.constant;

/**
 * 通用常量信息
 *
 * @author wesley.zhang
 */
public class JbmConstants {
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * RMI 远程方法调用
     */
    public static final String LOOKUP_RMI = "rmi:";

    /**
     * LDAP 远程方法调用
     */
    public static final String LOOKUP_LDAP = "ldap:";

    /**
     * LDAPS 远程方法调用
     */
    public static final String LOOKUP_LDAPS = "ldaps:";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * 登录成功状态
     */
    public static final String LOGIN_SUCCESS_STATUS = "0";

    /**
     * 登录失败状态
     */
    public static final String LOGIN_FAIL_STATUS = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 验证码有效期（分钟）
     */
    public static final long CAPTCHA_EXPIRATION = 2;


    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * 默认超级管理员账号
     */
    public final static String ROOT = "admin";

    /**
     * 短信验证码前缀
     */
    public final static String PIN_PREFIX = "PIN:";


    /**
     * 二维码登录授权码前缀
     */
    public final static String QR_PREFIX = "QR:";

    /**
     * 定时任务违规的字符
     */
    public static final String[] JOB_ERROR_STR = {"java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.jbm.cluster.common.core.utils.file"};

    /**
     * 默认接口分类
     */
    public final static String DEFAULT_API_CATEGORY = "default";

    /**
     * 状态:0-无效 1-有效
     */
    public final static int ENABLED = 1;
    public final static int DISABLED = 0;


    /**
     * 系统用户类型:超级管理员-super 普通管理员-admin
     */
    public final static String USER_TYPE_SUPER = "super";
    public final static String USER_TYPE_ADMIN = "admin";

    /**
     * 账号状态
     * 0:禁用、1:正常、2:锁定
     */
    public final static int ACCOUNT_STATUS_DISABLE = 0;
    public final static int ACCOUNT_STATUS_NORMAL = 1;
    public final static int ACCOUNT_STATUS_LOCKED = 2;

    /**
     * 账号类型:
     * username:系统用户名、email：邮箱、mobile：手机号、qq：QQ号、weixin：微信号、weibo：微博
     */
    public final static String ACCOUNT_TYPE_USERNAME = "username";
    public final static String ACCOUNT_TYPE_EMAIL = "email";
    public final static String ACCOUNT_TYPE_MOBILE = "mobile";

    /**
     * 账号域
     */
    public static final String ACCOUNT_DOMAIN_ADMIN = "@admin.com";
    public static final String ACCOUNT_DOMAIN_PORTAL = "@portal.com";
}
