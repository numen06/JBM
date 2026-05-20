package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.auth.AuthorityApi;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.cluster.api.entitys.basic.BaseAuthorityAction;
import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseAuthorityUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.center.business.BaseAuthorityBusiness;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统权限管理
 */
@Api(tags = "系统权限管理")
@RequestMapping("/authority")
@RestController
public class BaseAuthorityController {

    @Autowired
    private BaseAuthorityBusiness baseAuthorityBusiness;
    @Autowired
    private BaseUserBusiness baseUserBusiness;

    @ApiOperation(value = "访问权限资源")
    @GetMapping("/resources")
    public ResultBody<List<AuthorityResource>> listResources() {
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityResource());
    }

    @ApiOperation(value = "接口权限")
    @GetMapping("/apis")
    public ResultBody<List<AuthorityApi>> listApis(
            @RequestParam(value = "serviceId", required = false) String serviceId) {
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityApi(serviceId));
    }

    @ApiOperation(value = "菜单权限")
    @GetMapping("/menus")
    public ResultBody<List<AuthorityMenu>> listMenus() {
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityMenu(1));
    }

    @ApiOperation(value = "菜单权限树")
    @GetMapping("/menus/tree")
    public ResultBody<List<Map<String, Object>>> listMenuTree(
            @RequestParam(value = "appId", required = false) Long appId) {
        return ResultBody.callback(() -> {
            List<AuthorityMenu> result = baseAuthorityBusiness.findAuthorityMenu(1, appId);
            return ServiceUtils.listToTreeList(result, AuthorityMenu::getMenuId, AuthorityMenu::getParentId);
        }).msg("查询列表成功");
    }

    @ApiOperation(value = "功能权限")
    @GetMapping("/actions/{actionId}")
    public ResultBody<List<BaseAuthorityAction>> listActionAuthorities(@PathVariable Long actionId) {
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityAction(actionId));
    }

    @ApiOperation(value = "角色权限")
    @GetMapping("/roles/{roleId}")
    public ResultBody<List<OpenAuthority>> getRoleAuthorities(@PathVariable Long roleId) {
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityByRole(roleId));
    }

    @ApiOperation(value = "用户权限")
    @GetMapping("/users/{userId}")
    public ResultBody<List<OpenAuthority>> getUserAuthorities(@PathVariable Long userId) {
        com.jbm.cluster.api.entitys.basic.BaseUser user = baseUserBusiness.getUserById(userId);
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityByUserId(
                userId, JbmConstants.ROOT.equals(user.getUserName())));
    }

    @ApiOperation(value = "应用权限")
    @GetMapping("/apps/{appId}")
    public ResultBody<List<OpenAuthority>> getAppAuthorities(@PathVariable Long appId) {
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityByApp(appId));
    }

    @ApiOperation(value = "设置角色权限")
    @PutMapping("/roles/{roleId}")
    public ResultBody<Void> putRoleAuthorities(
            @PathVariable Long roleId,
            @RequestBody BaseAuthorityRoleForm form) {
        form.setRoleId(roleId);
        baseAuthorityBusiness.grantAuthorityRole(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "设置用户权限")
    @PutMapping("/users/{userId}")
    public ResultBody<Void> putUserAuthorities(
            @PathVariable Long userId,
            @RequestBody BaseAuthorityUserForm form) {
        form.setUserId(userId);
        baseAuthorityBusiness.grantAuthorityUser(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "设置应用权限")
    @PutMapping("/apps/{appId}")
    public ResultBody<Void> putAppAuthorities(
            @PathVariable Long appId,
            @RequestBody BaseAuthorityRoleForm form) {
        baseAuthorityBusiness.grantAuthorityApp(appId, form.getExpireTime(), form.getAuthorityIds());
        return ResultBody.ok();
    }

    @ApiOperation(value = "设置功能权限")
    @PutMapping("/actions/{actionId}")
    public ResultBody<Void> putActionAuthorities(
            @PathVariable Long actionId,
            @RequestBody BaseAuthorityRoleForm form) {
        baseAuthorityBusiness.grantAuthorityAction(actionId, form.getAuthorityIds());
        return ResultBody.ok();
    }
}
