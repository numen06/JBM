package com.jbm.cluster.common.satoken.utils;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.event.auth.LoginSuccessEvent;
import com.jbm.cluster.api.event.auth.LogoutEvent;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtContext;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtPrincipal;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtSupport;
import org.springframework.context.ApplicationEvent;
import com.jbm.cluster.core.constant.UserConstants;
import com.jbm.framework.exceptions.UtilException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录鉴权助手
 * <p>
 * user_type 为 用户类型 同一个用户表 可以有多种用户类型 例如 pc,app
 * deivce 为 设备类型 同一个用户类型 可以有 多种设备类型 例如 web,ios
 * 可以组成 用户类型与设备类型多对多的 权限灵活控制
 * <p>
 * 多用户体系 针对 多种用户类型 但权限控制不一致
 * 可以组成 多用户类型表与多设备类型 分别控制权限
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHelper {

    public static final String JOIN_CODE = ":";
    public static final String LOGIN_USER_KEY = "loginUser";

    private static final ThreadLocal<JbmLoginUser> LOGIN_CACHE = new ThreadLocal<>();
    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Map<String, StpLogic> userTypes = new HashMap<>();

    /**
     * 登录系统
     *
     * @param loginUser 登录用户信息
     */
    public static void login(JbmLoginUser loginUser) {
        login(loginUser, null, null, null, null);
    }

    public static void login(JbmLoginUser loginUser, String loginType, String clientId) {
        login(loginUser, loginType, clientId, null, null);
    }

    public static void login(JbmLoginUser loginUser, String loginType, String clientId, String ip, String userAgent) {
        String device = ObjectUtil.isNull(loginUser.getDevice()) ? RequestDeviceType.PC.getDevice() : loginUser.getDevice();
        loginByDevice(loginUser, device, loginType, clientId, ip, userAgent);
    }

    /**
     * 获取当前OAuthToken,登出
     */
    public static void loginout() {
        Object loginId = null;
        String tokenValue = null;
        JbmLoginUser loginUser = softGetLoginUser();
        try {
            tokenValue = StpUtil.getTokenValue();
            loginId = SaOAuth2Util.getLoginIdByAccessToken(tokenValue);
        } catch (Exception ignored) {
        }
        revokeOAuth2AccessTokenIfPresent(tokenValue);
        if (loginId != null) {
            StpUtil.logout(loginId);
        } else {
            try {
                StpUtil.logout();
            } catch (Exception ignored) {
            }
        }
        clearCache();
        publishEvent(new LogoutEvent(LoginHelper.class, loginId, loginUser, tokenValue));
    }

    public static void loginout(Object loginId) {
        JbmLoginUser loginUser = null;
        String tokenValue = null;
        try {
            tokenValue = StpUtil.getTokenValueByLoginId(loginId);
            loginUser = getLoginUser(loginId);
        } catch (Exception ignored) {
        }
        revokeOAuth2AccessTokenIfPresent(tokenValue);
        StpUtil.logout(loginId);
        clearCache();
        publishEvent(new LogoutEvent(LoginHelper.class, loginId, loginUser, tokenValue));
    }

    private static void revokeOAuth2AccessTokenIfPresent(String tokenValue) {
        if (StrUtil.isBlank(tokenValue)) {
            return;
        }
        try {
            SaOAuth2Util.revokeAccessToken(tokenValue);
        } catch (Exception ignored) {
        }
    }

    /**
     * 登录系统 基于 设备类型
     * 针对相同用户体系不同设备
     *
     * @param loginUser 登录用户信息
     */
    public static void loginByDevice(JbmLoginUser loginUser, String device) {
        loginByDevice(loginUser, device, null, null, null, null);
    }

    public static void loginByDevice(JbmLoginUser loginUser, String device, String loginType, String clientId,
                                     String ip, String userAgent) {
        LOGIN_CACHE.set(loginUser);
        StpUtil.login(loginUser.getLoginId(), device);
        loginUser.setToken(StpUtil.getTokenValue());
        setLoginUser(loginUser);
        publishEvent(new LoginSuccessEvent(LoginHelper.class, loginUser, loginType, clientId, device, ip, userAgent));
    }

    private static void publishEvent(ApplicationEvent event) {
        try {
            SpringUtil.publishEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取用户(多级缓存)
     */
    public static JbmLoginUser getLoginUser() {
        JbmLoginUser loginUser = LOGIN_CACHE.get();
        if (loginUser != null) {
            return loginUser;
        }
        RuntimeException failure = null;
        try {
            loginUser = (JbmLoginUser) StpUtil.getTokenSession().get(LOGIN_USER_KEY);
            if (loginUser != null) {
                return loginUser;
            }
        } catch (RuntimeException e) {
            failure = e;
        } catch (Exception ignored) {
        }
        loginUser = StandardJwtContext.getLoginUser();
        if (loginUser != null) {
            LOGIN_CACHE.set(loginUser);
            return loginUser;
        }
        if (failure != null) {
            throw failure;
        }
        return loginUser;
    }

    /**
     * 设置用户数据(多级缓存)
     */
    public static void setLoginUser(JbmLoginUser loginUser) {
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 仅写入请求级缓存（跨节点 OAuth/JWT 透传且 Redis 无 session 时使用）。
     */
    public static void setLoginUserCache(JbmLoginUser loginUser) {
        if (loginUser != null) {
            LOGIN_CACHE.set(loginUser);
        }
    }

    /**
     * 获取用户(多级缓存)
     */
    public static JbmLoginUser getLoginUser(Object loginId) {
        RuntimeException failure = null;
        try {
            String tokenValue = StpUtil.getTokenValueByLoginId(loginId);
            return (JbmLoginUser) StpUtil.getTokenSessionByToken(tokenValue).get(LOGIN_USER_KEY);
        } catch (RuntimeException e) {
            failure = e;
        } catch (Exception ignored) {
        }
        StandardJwtPrincipal principal = StandardJwtContext.get();
        if (principal != null && String.valueOf(loginId).equals(principal.getLoginId())) {
            return principal.getLoginUser();
        }
        if (failure != null) {
            throw failure;
        }
        return null;
    }

    /**
     * 初始化一级缓存 从Session中加载用户信息到ThreadLocal
     * 用于在请求开始时预加载用户信息，避免在某些场景下（如租户拦截器）获取不到用户
     */
    public static void initCache() {
        try {
            // 如果ThreadLocal已有数据，不重复加载
            if (LOGIN_CACHE.get() != null) {
                return;
            }
            JbmLoginUser standardJwtUser = StandardJwtContext.getLoginUser();
            if (standardJwtUser != null) {
                LOGIN_CACHE.set(standardJwtUser);
                return;
            }
            // 从Session中获取用户信息并设置到ThreadLocal
            JbmLoginUser loginUser = (JbmLoginUser) StpUtil.getTokenSession().get(LOGIN_USER_KEY);
            if (loginUser != null) {
                LOGIN_CACHE.set(loginUser);
            }
        } catch (Exception e) {
            // 忽略异常，可能是未登录状态
        }
    }

    /**
     * 安全获取用户对象
     *
     * @return
     */
    public static JbmLoginUser softGetLoginUser() {
        try {
            return getLoginUser();
        } catch (Exception e) {
            // 增加日志便于排查问题
            // log.debug("获取登录用户信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取用户(多级缓存)
     */
    public static JbmLoginUser getLoginUser(String tokenValue) {
        RuntimeException failure = null;
        try {
            return (JbmLoginUser) StpUtil.getTokenSessionByToken(tokenValue).get(LOGIN_USER_KEY);
        } catch (RuntimeException e) {
            failure = e;
        } catch (Exception ignored) {
        }
        StandardJwtPrincipal principal = StandardJwtContext.get();
        if (principal != null && tokenValue != null && tokenValue.equals(principal.getToken())) {
            return principal.getLoginUser();
        }
        principal = StandardJwtSupport.verify(tokenValue);
        if (principal != null) {
            return principal.getLoginUser();
        }
        if (failure != null) {
            throw failure;
        }
        return null;
    }

    /**
     * 清除一级缓存 防止内存问题
     */
    public static void clearCache() {
        LOGIN_CACHE.remove();
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        JbmLoginUser loginUser = getLoginUser();
        if (ObjectUtil.isNull(loginUser)) {
            String loginId = StpUtil.getLoginIdAsString();
            String userId = StrUtil.subAfter(loginId, JOIN_CODE, true);
//            for (UserType value : UserType.values()) {
//            if (StrUtil.contains(loginId, value.getUserType())) {
//                String[] strs = (StrUtil.splitToArray(loginId, JOIN_CODE));
//                // 用户id在总是在最后
//                userId = strs[strs.length - 1];
//            }
//            }
            if (StrUtil.isBlank(userId)) {
                throw new UtilException("登录用户: LoginId异常 => " + loginId);
            }
            return Long.parseLong(userId);
        }
        return loginUser.getUserId();
    }

    /**
     * 获取部门ID
     */
    public static Long getDeptId() {
        return getLoginUser().getDeptId();
    }

    /**
     * 获取用户所属公司/组织 ID（数据过滤范围）
     */
    public static Long getCompanyId() {
        JbmLoginUser loginUser = softGetLoginUser();
        return loginUser != null ? loginUser.getCompanyId() : null;
    }

    /**
     * 获取数据范围组织 ID，与 {@link #getCompanyId()} 一致
     */
    public static Long getOrgId() {
        return getCompanyId();
    }

//    /**
//     * 获取用户类型
//     */
//    public static UserType getUserType() {
//        String loginId = StpUtil.getLoginIdAsString();
//        return UserType.getUserType(loginId);
//    }

    /**
     * 获取用户账户
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 是否为超级管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId) {
        return UserConstants.ADMIN_ID.equals(userId);
    }

    public static boolean isAdmin() {
        JbmLoginUser login = softGetLoginUser();
        if (login != null) {
            if (com.jbm.cluster.core.constant.JbmConstants.ROOT.equals(login.getUsername())) {
                return true;
            }
            if (StrUtil.equalsIgnoreCase(com.jbm.cluster.core.constant.JbmConstants.ROOT_USER_NAME, login.getUsername())) {
                return true;
            }
            if ("super".equalsIgnoreCase(login.getUserType())) {
                return true;
            }
            if (login.getRoleIds() != null
                    && login.getRoleIds().contains(com.jbm.cluster.core.constant.JbmConstants.ROOT_ROLE_ID)) {
                return true;
            }
        }
        return isAdmin(getUserId());
    }

    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword     真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

}
