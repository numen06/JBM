package com.jbm.cluster.center.integration;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.cluster.api.form.BaseRoleForm;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.center.controller.BaseAppController;
import com.jbm.cluster.center.controller.BaseAuthorityController;
import com.jbm.cluster.center.controller.BaseDeveloperController;
import com.jbm.cluster.center.controller.BaseMenuController;
import com.jbm.cluster.center.controller.BaseRoleController;
import com.jbm.cluster.center.controller.BaseUserController;
import com.jbm.cluster.center.integration.support.CenterH2ApiTestSupport;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Center RBAC 接口逻辑验证（直接调用 Controller，H2 + 种子数据）。
 */
class CenterRbacApiH2IT extends CenterH2ApiTestSupport {

    @Autowired
    private BaseUserController baseUserController;
    @Autowired
    private BaseRoleController baseRoleController;
    @Autowired
    private BaseAuthorityController baseAuthorityController;
    @Autowired
    private BaseMenuController baseMenuController;
    @Autowired
    private BaseAppController baseAppController;
    @Autowired
    private BaseDeveloperController baseDeveloperController;

    @Test
    @DisplayName("种子数据：超管与权限/菜单查询")
    void seedData_adminAndAuthorityQueries() {
        ResultBody<BaseUser> user = baseUserController.getUser(JbmConstants.ROOT_USER_ID);
        assertSuccess(user);
        assertThat(user.getResult().getUserName()).isEqualTo(JbmConstants.ROOT_USER_NAME);

        ResultBody<List<com.jbm.cluster.api.model.auth.OpenAuthority>> authorities =
                baseAuthorityController.getUserAuthorities(JbmConstants.ROOT_USER_ID);
        assertSuccess(authorities);
        assertThat(authorities.getResult()).isNotEmpty();

        ResultBody<List<com.jbm.cluster.api.entitys.auth.AuthorityMenu>> menus =
                baseAuthorityController.listMenus();
        assertSuccess(menus);
        assertThat(menus.getResult()).isNotEmpty();

        ResultBody<List<BaseRole>> roleAll = baseRoleController.listAllRoles();
        assertSuccess(roleAll);
        assertThat(roleAll.getResult()).isNotEmpty();
        assertThat(roleAll.getResult().stream()
                .anyMatch(r -> "super_admin".equals(r.getRoleCode()))).isTrue();
    }

    @Test
    @DisplayName("角色：新增-查询-改-授权-删")
    void role_crudAndGrant() {
        String code = "it_role_" + System.nanoTime();
        BaseRoleForm addBody = JSON.parseObject(
                "{\"roleCode\":\"" + code + "\",\"roleName\":\"集成测试角色\",\"status\":1,\"roleDesc\":\"h2-it\"}",
                BaseRoleForm.class);

        ResultBody<Long> added = baseRoleController.createRole(addBody);
        assertSuccess(added);
        Long roleId = added.getResult();
        assertThat(roleId).isNotNull();

        ResultBody<BaseRole> info = baseRoleController.getRole(roleId);
        assertSuccess(info);
        assertThat(info.getResult().getRoleCode()).isEqualTo(code);

        BaseRoleForm updateBody = JSON.parseObject(
                "{\"roleId\":" + roleId + ",\"roleCode\":\"" + code
                        + "\",\"roleName\":\"集成测试角色-已改\",\"status\":1}",
                BaseRoleForm.class);
        ResultBody<?> updated = baseRoleController.updateRole(roleId, updateBody);
        assertSuccess(updated);

        ResultBody<List<com.jbm.cluster.api.model.auth.OpenAuthority>> adminAuth =
                baseAuthorityController.getUserAuthorities(JbmConstants.ROOT_USER_ID);
        assertSuccess(adminAuth);
        String authorityId = adminAuth.getResult().get(0).getAuthorityId();

        BaseAuthorityRoleForm grantForm = new BaseAuthorityRoleForm();
        grantForm.setAuthorityIds(new String[]{authorityId});
        ResultBody<?> grant = baseAuthorityController.putRoleAuthorities(roleId, grantForm);
        assertSuccess(grant);

        ResultBody<List<com.jbm.cluster.api.model.auth.OpenAuthority>> roleAuth =
                baseAuthorityController.getRoleAuthorities(roleId);
        assertSuccess(roleAuth);
        assertThat(roleAuth.getResult()).isNotEmpty();

        ResultBody<?> removed = baseRoleController.deleteRole(roleId);
        assertSuccess(removed);
    }

    @Test
    @DisplayName("菜单与按钮：新增-查-改-删")
    void menuAndAction_crud() {
        String menuCode = "it_menu_" + System.nanoTime();
        BaseMenuForm addBody = JSON.parseObject(
                "{\"menuCode\":\"" + menuCode + "\",\"menuName\":\"集成测试菜单\",\"status\":1,\"menuType\":1,\"parentId\":0}",
                BaseMenuForm.class);
        ResultBody<Long> menuAdded = baseMenuController.createMenu(addBody);
        assertSuccess(menuAdded);
        Long menuId = menuAdded.getResult();
        assertThat(menuId).isNotNull();

        ResultBody<com.jbm.cluster.api.entitys.basic.BaseMenu> menuInfo =
                baseMenuController.getMenu(menuId);
        assertSuccess(menuInfo);
        assertThat(menuInfo.getResult().getMenuCode()).isEqualTo(menuCode);

        BaseMenuForm updateBody = JSON.parseObject(
                "{\"menuId\":" + menuId + ",\"menuCode\":\"" + menuCode
                        + "\",\"menuName\":\"集成测试菜单-已改\",\"status\":1,\"menuType\":1,\"parentId\":0}",
                BaseMenuForm.class);
        ResultBody<Void> menuUpdated = baseMenuController.updateMenu(menuId, updateBody);
        assertSuccess(menuUpdated);

        ResultBody<Void> menuRemoved = baseMenuController.deleteMenu(menuId);
        assertSuccess(menuRemoved);
    }

    @Test
    @DisplayName("菜单：分页、关键字与 scope 过滤")
    void menu_paginationScopeAndKeyword() {
        BaseMenuForm pageForm = new BaseMenuForm();
        pageForm.setPageForm(JSON.parseObject("{\"currPage\":1,\"pageSize\":5}", com.jbm.framework.usage.paging.PageForm.class));

        ResultBody<DataPaging<com.jbm.cluster.api.entitys.basic.BaseMenu>> page1 =
                baseMenuController.listMenus(pageForm);
        assertSuccess(page1);
        DataPaging<com.jbm.cluster.api.entitys.basic.BaseMenu> paging = page1.getResult();
        assertThat(paging.getTotal()).isGreaterThan(0);
        assertThat(paging.getContents()).isNotEmpty();
        assertThat(paging.getContents().size()).isLessThanOrEqualTo(5);

        BaseMenuForm platformForm = new BaseMenuForm();
        platformForm.setScope("platform");
        platformForm.setPageForm(JSON.parseObject("{\"currPage\":1,\"pageSize\":50}", com.jbm.framework.usage.paging.PageForm.class));
        ResultBody<DataPaging<com.jbm.cluster.api.entitys.basic.BaseMenu>> platformPage =
                baseMenuController.listMenus(platformForm);
        assertSuccess(platformPage);
        assertThat(platformPage.getResult().getContents()).isNotEmpty();
        assertThat(platformPage.getResult().getContents().stream().allMatch(m -> m.getAppId() == null)).isTrue();

        BaseMenuForm keywordForm = new BaseMenuForm();
        keywordForm.setKeyword("用户");
        keywordForm.setPageForm(JSON.parseObject("{\"currPage\":1,\"pageSize\":20}", com.jbm.framework.usage.paging.PageForm.class));
        ResultBody<DataPaging<com.jbm.cluster.api.entitys.basic.BaseMenu>> keywordPage =
                baseMenuController.listMenus(keywordForm);
        assertSuccess(keywordPage);
        assertThat(keywordPage.getResult().getContents()).isNotEmpty();
        assertThat(keywordPage.getResult().getContents().stream()
                .anyMatch(m -> m.getMenuName() != null && m.getMenuName().contains("用户"))).isTrue();
    }

    @Test
    @DisplayName("应用与开发者：查询")
    void appAndDeveloper_queries() {
        ResultBody<com.jbm.cluster.api.entitys.basic.BaseApp> appByKey =
                baseAppController.getAppByApiKey(JbmConstants.SEED_DEV_APP_API_KEY);
        assertSuccess(appByKey);
        assertThat(appByKey.getResult().getApiKey()).isEqualTo(JbmConstants.SEED_DEV_APP_API_KEY);

        ResultBody<List<com.jbm.cluster.api.entitys.basic.BaseDeveloper>> devAll =
                baseDeveloperController.listAllDevelopers();
        assertSuccess(devAll);

        ResultBody<List<com.jbm.cluster.api.entitys.basic.BaseMenu>> menuAll =
                baseMenuController.listAllMenus(null);
        assertSuccess(menuAll);
        assertThat(menuAll.getResult()).isNotEmpty();
    }

    @Test
    @DisplayName("用户：列表与分页")
    void user_listAndPage() {
        ResultBody<?> list = baseUserController.listUsers(new BaseUserForm());
        assertSuccess(list);
        assertThat(((DataPaging<?>) list.getResult()).getContents()).isNotEmpty();

        BaseUserForm pageBody = JSON.parseObject(
                "{\"pageForm\":{\"pageNo\":1,\"pageSize\":10}}", BaseUserForm.class);
        ResultBody<?> page = baseUserController.listUsers(pageBody);
        assertSuccess(page);
        assertThat(((DataPaging<?>) page.getResult()).getContents()).isNotEmpty();
        assertThat(((DataPaging<?>) page.getResult()).getTotal()).isGreaterThan(0);
    }

    private static void assertSuccess(ResultBody<?> body) {
        assertThat(body).isNotNull();
        assertThat(body.getSuccess())
                .as("接口失败: %s", body.getMessage())
                .isTrue();
        assertThat(body.getCode()).isEqualTo(200);
    }
}
