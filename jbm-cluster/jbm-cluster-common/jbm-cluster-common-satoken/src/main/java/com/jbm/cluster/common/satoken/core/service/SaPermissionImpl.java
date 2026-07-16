package com.jbm.cluster.common.satoken.core.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * sa-token 权限管理实现类
 *
 * @author Lion Li
 */
public class SaPermissionImpl implements StpInterface {

    private static final List<String> SUPER_ADMIN_AUTHORITY = Collections.singletonList("*");

    /**
     * 获取菜单权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        JbmLoginUser loginUser = resolveLoginUser(loginId);
        if (loginUser == null) {
            return Collections.emptyList();
        }
        if (LoginHelper.isAdmin(loginUser.getUserId())) {
            return SUPER_ADMIN_AUTHORITY;
        }
        Set<String> menuPermission = loginUser.getMenuPermission();
        if (menuPermission == null || menuPermission.isEmpty()) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(menuPermission);
    }

    /**
     * 获取角色权限列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        JbmLoginUser loginUser = resolveLoginUser(loginId);
        if (loginUser == null) {
            return Collections.emptyList();
        }
        if (LoginHelper.isAdmin(loginUser.getUserId())) {
            return SUPER_ADMIN_AUTHORITY;
        }
        Set<String> roles = loginUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(roles);
    }

    private JbmLoginUser resolveLoginUser(Object loginId) {
        try {
            JbmLoginUser cached = LoginHelper.softGetLoginUser();
            if (cached != null && loginId != null
                    && StrUtil.equals(String.valueOf(loginId), cached.getLoginId())) {
                return cached;
            }
            return LoginHelper.getLoginUser(loginId);
        } catch (Exception e) {
            return null;
        }
    }
}
