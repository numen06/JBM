package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.auth.AuthorityApi;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.cluster.api.entitys.basic.BaseAuthorityAction;
import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseAuthorityUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.center.business.BaseAuthorityBusiness;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseUserService baseUserService;

    @ApiOperation(value = "访问权限资源")
    @GetMapping("/resources")
    public ResultBody<List<AuthorityResource>> listResources() {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityResource());
    }

    @ApiOperation(value = "接口权限")
    @GetMapping("/apis")
    public ResultBody<List<AuthorityApi>> listApis(
            @RequestParam(value = "serviceId", required = false) String serviceId) {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityApi(serviceId));
    }

    @ApiOperation(value = "当前用户可授权给 API Key 的接口权限")
    @GetMapping("/apis/grantable")
    public ResultBody<List<OpenAuthority>> listGrantableApis() {
        Long userId = LoginHelper.getUserId();
        com.jbm.cluster.api.entitys.basic.BaseUser user = baseUserService.getUserById(userId);
        boolean root = user != null && JbmConstants.isSuperUser(user.getUserId(), user.getUserName(), user.getUserType());
        return ResultBody.callback(() -> {
            List<OpenAuthority> authorities = baseAuthorityService.findAuthorityByUser(userId, root);
            if (authorities == null) {
                return Collections.emptyList();
            }
            return authorities.stream()
                    .filter(a -> a.getAuthority() != null
                            && a.getAuthority().startsWith(JbmSecurityConstants.AUTHORITY_PREFIX_API))
                    .collect(Collectors.toList());
        });
    }

    @ApiOperation(value = "菜单权限")
    @GetMapping("/menus")
    public ResultBody<List<AuthorityMenu>> listMenus() {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityMenu(1));
    }

    @ApiOperation(value = "权限目录（菜单+按钮，type=1；API 为 type=2）")
    @GetMapping("/catalog")
    public ResultBody<List<OpenAuthority>> listAuthorityCatalog(
            @RequestParam(value = "type", required = false, defaultValue = "1") String type) {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityByType(type));
    }

    @ApiOperation(value = "菜单权限树")
    @GetMapping("/menus/tree")
    public ResultBody<List<Map<String, Object>>> listMenuTree(
            @RequestParam(value = "appId", required = false) Long appId) {
        return ResultBody.callback(() -> {
            List<AuthorityMenu> result = baseAuthorityService.findAuthorityMenu(1, appId);
            return ServiceUtils.listToTreeList(result, AuthorityMenu::getMenuId, AuthorityMenu::getParentId);
        }).msg("查询列表成功");
    }

    @ApiOperation(value = "功能权限")
    @GetMapping("/actions/{actionId}")
    public ResultBody<List<BaseAuthorityAction>> listActionAuthorities(@PathVariable Long actionId) {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityAction(actionId));
    }

    @ApiOperation(value = "角色权限")
    @GetMapping("/roles/{roleId}")
    public ResultBody<List<OpenAuthority>> getRoleAuthorities(@PathVariable Long roleId) {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityByRole(roleId));
    }

    @ApiOperation(value = "用户权限")
    @GetMapping("/users/{userId}")
    public ResultBody<List<OpenAuthority>> getUserAuthorities(@PathVariable Long userId) {
        com.jbm.cluster.api.entitys.basic.BaseUser user = baseUserService.getUserById(userId);
        boolean root = user != null && JbmConstants.ROOT.equals(user.getUserName());
        return ResultBody.callback(() -> baseAuthorityBusiness.findAuthorityByUserId(userId, root));
    }

    @ApiOperation(value = "应用权限")
    @GetMapping("/apps/{appId}")
    public ResultBody<List<OpenAuthority>> getAppAuthorities(@PathVariable Long appId) {
        return ResultBody.callback(() -> baseAuthorityService.findAuthorityByApp(appId));
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
