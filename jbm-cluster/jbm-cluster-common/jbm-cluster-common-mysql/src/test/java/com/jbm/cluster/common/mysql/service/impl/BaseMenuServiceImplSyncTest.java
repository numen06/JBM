package com.jbm.cluster.common.mysql.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import com.jbm.cluster.common.mysql.service.BaseActionService;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.MenuDataScopeHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseMenuServiceImplSyncTest {

    private static final long SOURCE_APP_ID = 1000L;
    private static final long TARGET_APP_ID = 2000L;

    @Mock
    private BaseMenuMapper baseMenuMapper;
    @Mock
    private BaseAuthorityService baseAuthorityService;
    @Mock
    private BaseActionService baseActionService;
    @Mock
    private BaseAppService baseAppService;
    @Mock
    private MenuDataScopeHelper menuDataScopeHelper;

    private BaseMenuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BaseMenuServiceImpl();
        ReflectionTestUtils.setField(service, "baseMenuMapper", baseMenuMapper);
        ReflectionTestUtils.setField(service, "baseAuthorityService", baseAuthorityService);
        ReflectionTestUtils.setField(service, "baseActionService", baseActionService);
        ReflectionTestUtils.setField(service, "baseAppService", baseAppService);
        ReflectionTestUtils.setField(service, "menuDataScopeHelper", menuDataScopeHelper);
        ReflectionTestUtils.setField(service, "DEFAULT_SERVICE_ID", "jbm-cluster-platform-center");
        ReflectionTestUtils.setField(service, "self", service);
    }

    @Test
    void getMenuByCodeScopesByAppId() {
        BaseMenu jbmMenu = menu(1L, "dashboard", SOURCE_APP_ID);
        when(baseMenuMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(jbmMenu));

        BaseMenu found = service.getMenuByCode("dashboard", SOURCE_APP_ID);

        assertThat(found).isNotNull();
        assertThat(found.getAppId()).isEqualTo(SOURCE_APP_ID);
    }

    @Test
    void syncMenusFromAppClonesSourceMenusIntoTargetApp() {
        BaseApp sourceApp = new BaseApp();
        sourceApp.setAppId(SOURCE_APP_ID);
        BaseApp targetApp = new BaseApp();
        targetApp.setAppId(TARGET_APP_ID);
        when(baseAppService.getAppInfo(SOURCE_APP_ID)).thenReturn(sourceApp);
        when(baseAppService.getAppInfo(TARGET_APP_ID)).thenReturn(targetApp);

        BaseMenu root = menu(100L, "platform", SOURCE_APP_ID);
        root.setParentId(0L);
        BaseMenu child = menu(110L, "dashboard", SOURCE_APP_ID);
        child.setParentId(100L);
        when(baseMenuMapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            Wrapper<?> wrapper = invocation.getArgument(0);
            String sql = wrapper.getSqlSegment();
            if (sql != null && sql.contains("menu_id")) {
                return Collections.singletonList(child);
            }
            return Arrays.asList(root, child);
        });
        when(baseMenuMapper.selectById(any())).thenReturn(null);
        when(baseMenuMapper.insert(any(BaseMenu.class))).thenAnswer(invocation -> {
            BaseMenu inserted = invocation.getArgument(0);
            if (inserted.getMenuId() == null) {
                inserted.setMenuId(inserted.getMenuCode().equals("platform") ? 1001L : 1101L);
            }
            return 1;
        });
        when(baseActionService.findListByMenuId(any())).thenReturn(Collections.emptyList());

        int count = service.syncMenusFromApp(SOURCE_APP_ID, TARGET_APP_ID, "merge");

        assertThat(count).isGreaterThan(0);
        verify(baseMenuMapper, atLeastOnce()).insert(any(BaseMenu.class));
    }

    private static BaseMenu menu(Long menuId, String code, Long appId) {
        BaseMenu menu = new BaseMenu();
        menu.setMenuId(menuId);
        menu.setMenuCode(code);
        menu.setMenuName(code);
        menu.setAppId(appId);
        menu.setStatus(1);
        menu.setPriority(0);
        menu.setPath("/" + code);
        return menu;
    }
}
