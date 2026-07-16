package com.jbm.cluster.common.satoken.core.service;

import cn.dev33.satoken.strategy.SaStrategy;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.UserConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaPermissionImplTest {

    private final SaPermissionImpl saPermissionImpl = new SaPermissionImpl();

    @AfterEach
    void tearDown() {
        LoginHelper.clearCache();
    }

    @Test
    void superAdminHasAllPermissionsEvenWhenPermissionSetIsEmpty() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(UserConstants.ADMIN_ID);
        setLoginCache(loginUser);

        List<String> permissions = saPermissionImpl.getPermissionList(loginUser.getLoginId(), "login");

        assertEquals(Collections.singletonList("*"), permissions);
        assertTrue(SaStrategy.me.hasElement.apply(permissions, "ACTION_monitor:online:list"));
    }

    @Test
    void superAdminHasAllRolesEvenWhenRoleSetIsEmpty() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(UserConstants.ADMIN_ID);
        setLoginCache(loginUser);

        List<String> roles = saPermissionImpl.getRoleList(loginUser.getLoginId(), "login");

        assertEquals(Collections.singletonList("*"), roles);
        assertTrue(SaStrategy.me.hasElement.apply(roles, "admin"));
    }

    @Test
    void regularUserWithoutPermissionsCannotMatchProtectedPermission() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(10001L);
        setLoginCache(loginUser);

        List<String> permissions = saPermissionImpl.getPermissionList(loginUser.getLoginId(), "login");

        assertTrue(permissions.isEmpty());
        assertFalse(SaStrategy.me.hasElement.apply(permissions, "ACTION_monitor:online:list"));
    }

    @Test
    void regularUserWithoutRolesCannotMatchProtectedRole() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(10001L);
        setLoginCache(loginUser);

        List<String> roles = saPermissionImpl.getRoleList(loginUser.getLoginId(), "login");

        assertTrue(roles.isEmpty());
        assertFalse(SaStrategy.me.hasElement.apply(roles, "admin"));
    }

    private static JbmLoginUser buildLoginUser(Long userId) {
        JbmLoginUser loginUser = new JbmLoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername("sa-permission-test");
        loginUser.setRoles(Collections.emptySet());
        loginUser.setMenuPermission(Collections.emptySet());
        return loginUser;
    }

    private static void setLoginCache(JbmLoginUser loginUser) throws Exception {
        Field field = LoginHelper.class.getDeclaredField("LOGIN_CACHE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<JbmLoginUser> cache = (ThreadLocal<JbmLoginUser>) field.get(null);
        cache.set(loginUser);
    }
}
