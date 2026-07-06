package com.jbm.test.token;

import cn.dev33.satoken.SaManager;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.core.service.SaPermissionImpl;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sa-Token 权限/角色提供者单元测试
 */
class SaPermissionImplTest {

    private final SaPermissionImpl saPermissionImpl = new SaPermissionImpl();

    @BeforeEach
    void setUp() {
        SaManager.setStpInterface(saPermissionImpl);
    }

    @AfterEach
    void tearDown() {
        LoginHelper.clearCache();
    }

    @Test
    void getPermissionList_returnsEmptyWhenLoginUserMissing() {
        List<String> permissions = saPermissionImpl.getPermissionList("missing:user:1", "login");
        assertTrue(permissions.isEmpty());
    }

    @Test
    void getRoleList_returnsEmptyWhenLoginUserMissing() {
        List<String> roles = saPermissionImpl.getRoleList("missing:user:1", "login");
        assertTrue(roles.isEmpty());
    }

    @Test
    void getRoleList_readsRolesFromCachedLoginUser() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(
                Collections.singleton("admin"),
                Collections.singleton("ACTION_demo:read")
        );
        setLoginCache(loginUser);

        List<String> roles = saPermissionImpl.getRoleList(loginUser.getLoginId(), "login");
        assertEquals(Collections.singletonList("admin"), roles);
    }

    @Test
    void getPermissionList_readsMenuPermissionFromCachedLoginUser() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(
                Collections.singleton("admin"),
                new HashSet<>(Arrays.asList("ACTION_demo:read", "ACTION_demo:write"))
        );
        setLoginCache(loginUser);

        List<String> permissions = saPermissionImpl.getPermissionList(loginUser.getLoginId(), "login");
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("ACTION_demo:read"));
        assertTrue(permissions.contains("ACTION_demo:write"));
    }

    @Test
    void getRoleList_returnsEmptyWhenRolesMissing() throws Exception {
        JbmLoginUser loginUser = buildLoginUser(null, Collections.singleton("ACTION_demo:read"));
        setLoginCache(loginUser);

        List<String> roles = saPermissionImpl.getRoleList(loginUser.getLoginId(), "login");
        assertTrue(roles.isEmpty());
    }

    private static JbmLoginUser buildLoginUser(Set<String> roles, Set<String> menuPermission) {
        JbmLoginUser loginUser = new JbmLoginUser();
        loginUser.setUserId(99999L);
        loginUser.setUsername("sa-permission-test");
        loginUser.setRoles(roles);
        loginUser.setMenuPermission(menuPermission);
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
