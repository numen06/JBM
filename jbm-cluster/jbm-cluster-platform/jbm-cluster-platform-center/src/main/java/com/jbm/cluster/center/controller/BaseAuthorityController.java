package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseAuthorityUserForm;
import com.jbm.cluster.api.model.AuthorityApi;
import com.jbm.cluster.api.model.AuthorityMenu;
import com.jbm.cluster.api.model.AuthorityResource;
import com.jbm.cluster.api.model.entity.BaseAuthorityAction;
import com.jbm.cluster.api.model.entity.BaseAuthorityApp;
import com.jbm.cluster.api.model.entity.BaseUser;
import com.jbm.cluster.api.service.IBaseAuthorityServiceClient;
import com.jbm.cluster.center.service.BaseAuthorityService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.constants.CommonConstants;
import com.jbm.cluster.common.security.OpenAuthority;
import com.jbm.cluster.common.security.http.OpenRestTemplate;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author: liuyadu
 * @date: 2018/11/26 18:20
 * @description:
 */
@Api(tags = "系统权限管理")
@RestController
public class BaseAuthorityController implements IBaseAuthorityServiceClient {

    @Autowired
    private BaseAuthorityService baseAuthorityService;
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private OpenRestTemplate openRestTemplate;

    /**
     * 获取所有访问权限列表
     *
     * @return
     */
    @ApiOperation(value = "获取所有访问权限列表", notes = "获取所有访问权限列表")
    @GetMapping("/authority/access")
    @Override
    public ResultBody<List<AuthorityResource>> findAuthorityResource() {
        List<AuthorityResource> result = baseAuthorityService.findAuthorityResource();
        return ResultBody.ok().data(result);
    }

    /**
     * 获取权限列表
     *
     * @return
     */
    @ApiOperation(value = "获取接口权限列表", notes = "获取接口权限列表")
    @GetMapping("/authority/api")
    public ResultBody<List<AuthorityApi>> findAuthorityApi(
            @RequestParam(value = "serviceId", required = false) String serviceId
    ) {
        List<AuthorityApi> result = baseAuthorityService.findAuthorityApi(serviceId);
        return ResultBody.ok().data(result);
    }


    /**
     * 获取菜单权限列表
     *
     * @return
     */
    @ApiOperation(value = "获取菜单权限列表", notes = "获取菜单权限列表")
    @GetMapping("/authority/menu")
    @Override
    public ResultBody<List<AuthorityMenu>> findAuthorityMenu() {
        List<AuthorityMenu> result = baseAuthorityService.findAuthorityMenu(1);
        return ResultBody.ok().data(result);
    }

    /**
     * 获取菜单权限树结构列表
     *
     * @return
     */
    @ApiOperation(value = "获取菜单权限列表", notes = "获取菜单权限列表")
    @GetMapping("/authority/menu/treeList")
    public ResultBody<List<AuthorityMenu>> findAuthorityMenuTreeList() {
        List<AuthorityMenu> result = baseAuthorityService.findAuthorityMenu(1);
        List<Map<String, Object>> result2 = ServiceUtils.listToTreeList(result, AuthorityMenu::getMenuId, AuthorityMenu::getParentId);
        return ResultBody.ok().data(result2).msg("查询列表成功");
    }

    /**
     * 获取功能权限列表
     *
     * @param actionId
     * @return
     */
    @ApiOperation(value = "获取功能权限列表", notes = "获取功能权限列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "actionId", required = true, value = "功能按钮ID", paramType = "form")
    })
    @GetMapping("/authority/action")
    public ResultBody<List<BaseAuthorityAction>> findAuthorityAction(
            @RequestParam(value = "actionId") Long actionId
    ) {
        List<BaseAuthorityAction> list = baseAuthorityService.findAuthorityAction(actionId);
        return ResultBody.ok().data(list);
    }


    /**
     * 获取角色已分配权限
     *
     * @param roleId 角色ID
     * @return
     */
    @ApiOperation(value = "获取角色已分配权限", notes = "获取角色已分配权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "form")
    })
    @GetMapping("/authority/role")
    public ResultBody<List<OpenAuthority>> findAuthorityRole(Long roleId) {
        List<OpenAuthority> result = baseAuthorityService.findAuthorityByRole(roleId);
        return ResultBody.ok().data(result);
    }

    /**
     * 获取角色已分配权限
     *
     * @return
     */
    @ApiOperation(value = "获取角色已分配权限JSON方式", notes = "获取角色已分配权限")
    @PostMapping("/authority/byRole")
    public ResultBody<List<OpenAuthority>> findAuthorityRoles(@RequestBody(required = false) BaseAuthorityRoleForm baseAuthorityRoleForm) {
        List<OpenAuthority> result = baseAuthorityService.findAuthorityByRole(baseAuthorityRoleForm.getRoleId());
        return ResultBody.ok().data(result);
    }


    /**
     * 获取用户已分配权限
     *
     * @return
     */
    @ApiOperation(value = "获取用户已分配权限JSON", notes = "获取用户已分配权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", defaultValue = "", required = true, paramType = "form")
    })
    @PostMapping("/authority/byUser")
    public ResultBody<List<OpenAuthority>> findAuthorityUser(@RequestBody(required = false) BaseAuthorityUserForm baseAuthorityUserForm) {
        BaseUser user = baseUserService.getUserById(baseAuthorityUserForm.getUserId());
        List<OpenAuthority> result = baseAuthorityService.findAuthorityByUser(user.getUserId(), CommonConstants.ROOT.equals(user.getUserName()));
        return ResultBody.ok().data(result);
    }

    /**
     * 获取用户已分配权限
     *
     * @param userId 用户ID
     * @return
     */
    @ApiOperation(value = "获取用户已分配权限", notes = "获取用户已分配权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", defaultValue = "", required = true, paramType = "form")
    })
    @GetMapping("/authority/user")
    public ResultBody<List<OpenAuthority>> findAuthorityUser(
            @RequestParam(value = "userId") Long userId
    ) {
        BaseUser user = baseUserService.getUserById(userId);
        List<OpenAuthority> result = baseAuthorityService.findAuthorityByUser(userId, CommonConstants.ROOT.equals(user.getUserName()));
        return ResultBody.ok().data(result);
    }


    /**
     * 获取应用已分配接口权限
     *
     * @param appId 角色ID
     * @return
     */
    @ApiOperation(value = "获取应用已分配接口权限", notes = "获取应用已分配接口权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用Id", defaultValue = "", required = true, paramType = "form")
    })
    @GetMapping("/authority/app")
    public ResultBody<List<OpenAuthority>> findAuthorityApp(
            @RequestParam(value = "appId") String appId
    ) {
        List<OpenAuthority> result = baseAuthorityService.findAuthorityByApp(appId);
        return ResultBody.ok().data(result);
    }

    /**
     * 获取应用已分配接口权限
     *
     * @param appId 角色ID
     * @return
     */
    @ApiOperation(value = "获取应用已分配接口权限", notes = "获取应用已分配接口权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用Id", defaultValue = "", required = true, paramType = "form")
    })
    @PostMapping("/authority/byApp")
    public ResultBody<List<OpenAuthority>> findAuthorityApp(@RequestBody(required = false) BaseAuthorityApp baseAuthorityApp) {
        List<OpenAuthority> result = baseAuthorityService.findAuthorityByApp(baseAuthorityApp.getAppId());
        return ResultBody.ok().data(result);
    }

    /**
     * 分配角色权限
     *
     * @param roleId       角色ID
     * @param expireTime   授权过期时间
     * @param authorityIds 权限ID.多个以,隔开
     * @return
     */
    @ApiOperation(value = "分配角色权限", notes = "分配角色权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "expireTime", value = "过期时间.选填", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(name = "authorityIds", value = "权限ID.多个以,隔开.选填", defaultValue = "", required = false, paramType = "form")
    })
    @PostMapping("/authority/role/grant")
    public ResultBody grantAuthorityRole(
            @RequestParam(value = "roleId") Long roleId,
            @RequestParam(value = "expireTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date expireTime,
            @RequestParam(value = "authorityIds", required = false) String authorityIds
    ) {
        baseAuthorityService.addAuthorityRole(roleId, expireTime, StringUtils.isNotBlank(authorityIds) ? authorityIds.split(",") : new String[]{});
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 分配角色权限
     *
     * @return
     */
    @ApiOperation(value = "分配角色权限JSON提交方式", notes = "分配角色权限")
    @PostMapping("/authority/grant/role")
    public ResultBody grantAuthorityRole(@RequestBody(required = false) BaseAuthorityRoleForm baseAuthorityRoleForm) {
        baseAuthorityService.addAuthorityRole(baseAuthorityRoleForm.getRoleId(), baseAuthorityRoleForm.getExpireTime(), baseAuthorityRoleForm.getAuthorityIds());
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 分配用户权限
     *
     * @param userId       用户ID
     * @param expireTime   授权过期时间
     * @param authorityIds 权限ID.多个以,隔开
     * @return
     */
    @ApiOperation(value = "分配用户权限", notes = "分配用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "expireTime", value = "过期时间.选填", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(name = "authorityIds", value = "权限ID.多个以,隔开.选填", defaultValue = "", required = false, paramType = "form")
    })
    @PostMapping("/authority/user/grant")
    public ResultBody grantAuthorityUser(
            @RequestParam(value = "userId") Long userId,
            @RequestParam(value = "expireTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date expireTime,
            @RequestParam(value = "authorityIds", required = false) String authorityIds
    ) {
        baseAuthorityService.addAuthorityUser(userId, expireTime, StringUtils.isNotBlank(authorityIds) ? authorityIds.split(",") : new String[]{});
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }

    @ApiOperation(value = "分配用户权限JSON提交方式", notes = "分配用户权限")
    @PostMapping("/authority/grant/user")
    public ResultBody grantAuthorityUser(@RequestBody(required = false) BaseAuthorityUserForm baseAuthorityUserForm) {
        baseAuthorityService.addAuthorityUser(baseAuthorityUserForm.getUserId(), baseAuthorityUserForm.getExpireTime(), baseAuthorityUserForm.getAuthorityIds());
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 分配应用权限
     *
     * @param appId        应用Id
     * @param expireTime   授权过期时间
     * @param authorityIds 权限ID.多个以,隔开
     * @return
     */
    @ApiOperation(value = "分配应用权限", notes = "分配应用权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用Id", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "expireTime", value = "过期时间.选填", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(name = "authorityIds", value = "权限ID.多个以,隔开.选填", defaultValue = "", required = false, paramType = "form")
    })
    @PostMapping("/authority/app/grant")
    public ResultBody grantAuthorityApp(
            @RequestParam(value = "appId") String appId,
            @RequestParam(value = "expireTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date expireTime,
            @RequestParam(value = "authorityIds", required = false) String authorityIds
    ) {
        baseAuthorityService.addAuthorityApp(appId, expireTime, StringUtils.isNotBlank(authorityIds) ? authorityIds.split(",") : new String[]{});
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }

    /**
     * 功能按钮绑定API
     *
     * @param actionId
     * @param authorityIds
     * @return
     */
    @ApiOperation(value = "功能按钮授权", notes = "功能按钮授权")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "actionId", required = true, value = "功能按钮ID", paramType = "form"),
            @ApiImplicitParam(name = "authorityIds", required = false, value = "全新ID:多个用,号隔开", paramType = "form"),
    })
    @PostMapping("/authority/action/grant")
    public ResultBody grantAuthorityAction(
            @RequestParam(value = "actionId") Long actionId,
            @RequestParam(value = "authorityIds", required = false) String authorityIds
    ) {
        baseAuthorityService.addAuthorityAction(actionId, StringUtils.isNotBlank(authorityIds) ? authorityIds.split(",") : new String[]{});
        openRestTemplate.refreshGateway();
        return ResultBody.ok();
    }


}
