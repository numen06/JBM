package com.jbm.cluster.common.mysql.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jbm.cluster.api.entitys.basic.BaseAction;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import com.jbm.cluster.common.mysql.service.BaseActionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseMenuServiceImplTest {

    @Mock
    private BaseMenuMapper baseMenuMapper;
    @Mock
    private BaseActionService baseActionService;

    @Test
    void parsesLegacyActionListFromMenuJson() {
        String json = "[{\"menuId\":\"100\",\"menuCode\":\"user\",\"menuName\":\"用户管理\","
                + "\"actionList\":[{\"actionId\":\"200\",\"actionCode\":\"user_add\",\"actionName\":\"新增\"}]}]";

        List<BaseMenu> menus = JSON.parseArray(json, BaseMenu.class);

        assertThat(menus).hasSize(1);
        assertThat(menus.get(0).getImportActionList()).hasSize(1);
        assertThat(menus.get(0).getImportActionList().get(0).getActionCode()).isEqualTo("user_add");

        String exported = JSON.toJSONString(menus);
        assertThat(exported).contains("\"actionList\"");
        assertThat(exported).doesNotContain("importActionList");
    }

    @Test
    void importMenusSkipsExistingPersistMenuAndItsActions() {
        BaseMenuServiceImpl service = new BaseMenuServiceImpl();
        ReflectionTestUtils.setField(service, "baseMenuMapper", baseMenuMapper);
        ReflectionTestUtils.setField(service, "baseActionService", baseActionService);

        BaseMenu existing = new BaseMenu();
        existing.setMenuId(10L);
        existing.setMenuCode("system");
        existing.setMenuName("系统管理");
        existing.setParentId(0L);
        existing.setIsPersist(true);
        when(baseMenuMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(existing));

        BaseAction importAction = new BaseAction();
        importAction.setActionCode("system_add");
        importAction.setActionName("新增");

        BaseMenu importMenu = new BaseMenu();
        importMenu.setMenuId(100L);
        importMenu.setMenuCode("system");
        importMenu.setMenuName("旧系统管理");
        importMenu.setImportActionList(Collections.singletonList(importAction));

        int count = service.importMenus(Collections.singletonList(importMenu));

        assertThat(count).isZero();
        verify(baseMenuMapper, never()).insert(any(BaseMenu.class));
        verify(baseMenuMapper, never()).updateById(any(BaseMenu.class));
        verify(baseActionService, never()).addAction(any(BaseAction.class));
        verify(baseActionService, never()).updateAction(any(BaseAction.class));
    }
}
