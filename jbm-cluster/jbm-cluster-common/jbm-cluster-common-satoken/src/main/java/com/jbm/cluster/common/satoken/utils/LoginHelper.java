package com.jbm.cluster.common.satoken.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.constants.UserType;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.core.constant.UserConstants;
import com.jbm.framework.exceptions.UtilException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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

    /**
     * 登录系统
     *
     * @param loginUser 登录用户信息
     */
    public static void login(JbmLoginUser loginUser) {
        LOGIN_CACHE.set(loginUser);
        StpUtil.login(loginUser.getLoginId());
        setLoginUser(loginUser);
    }

    /**
     * 登录系统 基于 设备类型
     * 针对相同用户体系不同设备
     *
     * @param loginUser 登录用户信息
     */
    public static void loginByDevice(JbmLoginUser loginUser, RequestDeviceType deviceType) {
        LOGIN_CACHE.set(loginUser);
        StpUtil.login(loginUser.getLoginId(), deviceType.getDevice());
        setLoginUser(loginUser);
    }

    /**
     * 设置用户数据(多级缓存)
     */
    public static void setLoginUser(JbmLoginUser loginUser) {
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取用户(多级缓存)
     */
    public static JbmLoginUser getLoginUser() {
        JbmLoginUser loginUser = LOGIN_CACHE.get();
        if (loginUser != null) {
            return loginUser;
        }
        return (JbmLoginUser) StpUtil.getTokenSession().get(LOGIN_USER_KEY);
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
            String userId = null;
            for (UserType value : UserType.values()) {
                if (StrUtil.contains(loginId, value.getUserType())) {
                    String[] strs = (StrUtil.splitToArray(loginId, JOIN_CODE));
                    // 用户id在总是在最后
                    userId = strs[strs.length - 1];
                }
            }
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
     * 获取用户账户
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取用户类型
     */
    public static UserType getUserType() {
        String loginId = StpUtil.getLoginIdAsString();
        return UserType.getUserType(loginId);
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
        return isAdmin(getUserId());
    }

}
