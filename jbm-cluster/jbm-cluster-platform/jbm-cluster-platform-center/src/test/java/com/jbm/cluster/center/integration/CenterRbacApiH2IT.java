package com.jbm.cluster.center.integration;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
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
        ResultBody<BaseUser> user = baseUserController.getUserInfoById(JbmConstants.ROOT_USER_ID);
        assertSuccess(user);
        assertThat(user.getResult().getUserName()).isEqualTo(JbmConstants.ROOT_USER_NAME);

        ResultBody<List<com.jbm.cluster.api.model.auth.OpenAuthority>> authorities =
                baseAuthorityController.findAuthorityUser(JbmConstants.ROOT_USER_ID);
        assertSuccess(authorities);
        assertThat(authorities.getResult()).isNotEmpty();

        ResultBody<List<com.jbm.cluster.api.entitys.auth.AuthorityMenu>> menus =
                baseAuthorityController.findAuthorityMenu();
        assertSuccess(menus);
        assertThat(menus.getResult()).isNotEmpty();

        ResultBody<List<BaseRole>> roleAll = baseRoleController.getRoleAllList();
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

        ResultBody<Long> added = baseRoleController.addRole(addBody);
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
        ResultBody<?> updated = baseRoleController.updateRole(updateBody);
        assertSuccess(updated);

        ResultBody<List<com.jbm.cluster.api.model.auth.OpenAuthority>> adminAuth =
                baseAuthorityController.findAuthorityUser(JbmConstants.ROOT_USER_ID);
        assertSuccess(adminAuth);
        String authorityId = adminAuth.getResult().get(0).getAuthorityId();

        ResultBody<?> grant = baseAuthorityController.grantAuthorityRole(
                roleId, null, authorityId);
        assertSuccess(grant);

        ResultBody<List<com.jbm.cluster.api.model.auth.OpenAuthority>> roleAuth =
                baseAuthorityController.findAuthorityRole(roleId);
        assertSuccess(roleAuth);
        assertThat(roleAuth.getResult()).isNotEmpty();

        BaseRoleForm removeBody = JSON.parseObject("{\"roleId\":" + roleId + "}", BaseRoleForm.class);
        ResultBody<?> removed = baseRoleController.removeRole(removeBody);
        assertSuccess(removed);
    }

    @Test
    @DisplayName("菜单与按钮：新增-查-改-删")
    void menuAndAction_crud() {
        String menuCode = "it_menu_" + System.nanoTime();
        ResultBody<Long> menuAdded = baseMenuController.addMenu(
                menuCode, "集成测试菜单", null, "/", "", "_self", 1, 0L, 0, "", 1);
        assertSuccess(menuAdded);
        Long menuId = menuAdded.getResult();
        assertThat(menuId).isNotNull();

        ResultBody<com.jbm.cluster.api.entitys.basic.BaseMenu> menuInfo =
                baseMenuController.getMenu(menuId);
        assertSuccess(menuInfo);
        assertThat(menuInfo.getResult().getMenuCode()).isEqualTo(menuCode);

        ResultBody<?> menuUpdated = baseMenuController.updateMenu(
                menuId, menuCode, "集成测试菜单-已改", null, "/", "", "_self", 1, 0L, 0, "", 1);
        assertSuccess(menuUpdated);

        ResultBody<Boolean> menuRemoved = baseMenuController.removeMenu(menuId);
        assertSuccess(menuRemoved);
    }

    @Test
    @DisplayName("应用与开发者：查询")
    void appAndDeveloper_queries() {
        ResultBody<com.jbm.cluster.api.entitys.basic.BaseApp> appByKey =
                baseAppController.getAppByKey(JbmConstants.SEED_DEV_APP_API_KEY);
        assertSuccess(appByKey);
        assertThat(appByKey.getResult().getApiKey()).isEqualTo(JbmConstants.SEED_DEV_APP_API_KEY);

        ResultBody<List<com.jbm.cluster.api.entitys.basic.BaseDeveloper>> devAll =
                baseDeveloperController.getUserAllList();
        assertSuccess(devAll);

        ResultBody<List<com.jbm.cluster.api.entitys.basic.BaseMenu>> menuAll =
                baseMenuController.getMenuAllList(null);
        assertSuccess(menuAll);
        assertThat(menuAll.getResult()).isNotEmpty();
    }

    @Test
    @DisplayName("用户：列表与分页")
    void user_listAndPage() {
        ResultBody<List<BaseUser>> list = baseUserController.list(new BaseUserForm());
        assertSuccess(list);
        assertThat(list.getResult()).isNotEmpty();

        BaseUserForm pageBody = JSON.parseObject(
                "{\"pageForm\":{\"pageNo\":1,\"pageSize\":10}}", BaseUserForm.class);
        ResultBody<DataPaging<BaseUser>> page = baseUserController.pageList(pageBody);
        assertSuccess(page);
        assertThat(page.getResult().getContents()).isNotEmpty();
        assertThat(page.getResult().getTotal()).isGreaterThan(0);
    }

    private static void assertSuccess(ResultBody<?> body) {
        assertThat(body).isNotNull();
        assertThat(body.getSuccess())
                .as("接口失败: %s", body.getMessage())
                .isTrue();
        assertThat(body.getCode()).isEqualTo(200);
    }
}
