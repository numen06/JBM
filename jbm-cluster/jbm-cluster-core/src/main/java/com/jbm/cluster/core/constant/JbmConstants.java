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
     * 默认超级管理员用户 ID（与种子数据、{@link com.jbm.cluster.core.constant.UserConstants#ADMIN_ID} 一致）
     */
    public static final Long ROOT_USER_ID = 1L;

    /**
     * 默认超级管理员登录名
     */
    public static final String ROOT_USER_NAME = "admin";

    /**
     * 默认超级管理员账号（登录名别名，兼容历史代码）
     */
    public final static String ROOT = ROOT_USER_NAME;

    /**
     * H2 / 开发环境默认超管密码（生产环境请首次登录后修改）
     */
    public static final String ROOT_DEFAULT_PASSWORD = "admin";

    /**
     * 种子超级管理员角色 ID
     */
    public static final Long ROOT_ROLE_ID = 1L;

    /**
     * 是否超级管理员用户（登录名 admin、userType super 或种子 ROOT 用户 ID）
     */
    public static boolean isSuperUser(Long userId, String userName, String userType) {
        if (ROOT_USER_ID.equals(userId)) {
            return true;
        }
        if (userName != null && ROOT_USER_NAME.equalsIgnoreCase(userName)) {
            return true;
        }
        return userType != null && "super".equalsIgnoreCase(userType);
    }

    /**
     * H2 集成测试用开发者应用 apiKey（须在 base_app 中存在）
     */
    public static final String SEED_DEV_APP_API_KEY = "jbmSeedDevAppKey00000001";

    /**
     * H2 集成测试用开发者应用明文密钥（入库前 BCrypt；OAuth 校验走 {@code OAuthClientSecretVerifier}）
     */
    public static final String SEED_DEV_APP_SECRET = "jbmSeedDevSecret0000000001";

    /**
     * JBM 基础应用 apiKey（即 SEED_DEV_APP_API_KEY 的语义别名，供明文登录白名单等场景引用）
     */
    public static final String JBM_APP_API_KEY = SEED_DEV_APP_API_KEY;

    /**
     * JBM 基础应用明文密钥
     */
    public static final String JBM_APP_SECRET = SEED_DEV_APP_SECRET;

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
    public final static String USER_TYPE_NORMAL = "normal";

    /**
     * 验证账号的正则表达式
     */
    public final static String ACCOUNT_REGEX = "^.{5,16}$";

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
