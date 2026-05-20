package com.jbm.cluster.center.controller;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseRoleUser;
import com.jbm.cluster.api.form.BaseRoleForm;
import com.jbm.cluster.common.mysql.service.BaseRoleService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Api(tags = "系统角色管理")
@RestController
@RequestMapping("/role")
public class BaseRoleController extends BaseController {
    @Autowired
    private BaseRoleService baseRoleService;

    /**
     * 获取分页角色列表
     *
     * @return
     */
    @ApiOperation(value = "获取分页角色列表", notes = "获取分页角色列表")
    @PostMapping("")
    public ResultBody<DataPaging<BaseRole>> getRoleListPage(@RequestBody(required = false) BaseRoleForm form) {
        return ResultBody.callback(() -> baseRoleService.findListPage(form));
    }

    /**
     * 获取所有角色列表
     *
     * @return
     */
    @ApiOperation(value = "获取所有角色列表", notes = "获取所有角色列表")
    @PostMapping("/all")
    public ResultBody<List<BaseRole>> getRoleAllList() {
        return ResultBody.callback(() -> baseRoleService.findAllList());
    }

    /**
     * 获取角色详情
     *
     * @param roleId
     * @return
     */
    @ApiOperation(value = "获取角色详情", notes = "获取角色详情")
    @ApiImplicitParams({
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "path")
    })
    @PostMapping("/{roleId}/info")
    public ResultBody<BaseRole> getRole(@PathVariable(value = "roleId") Long roleId) {
        BaseRole result = baseRoleService.getRole(roleId);
        return ResultBody.callback(() -> result);
    }

    /**
     * 添加角色
     *
     * @return
     */
    @ApiOperation(value = "添加角色", notes = "添加角色")
    @ApiImplicitParams({
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleCode", value = "角色编码", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleName", value = "角色显示名称", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleDesc", value = "描述", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form")
    })
    @PostMapping("/add")
    public ResultBody<Long> addRole(@RequestBody(required = false) BaseRoleForm form) {
        return ResultBody.callback(() -> {
            Long roleId = null;
            BaseRole result = baseRoleService.addRole(form);
            if (result != null) {
                roleId = result.getRoleId();
            }
            return roleId;
        });
    }

    /**
     * 编辑角色
     *
     * @return
     */
    @ApiOperation(value = "编辑角色", notes = "编辑角色")
    @ApiImplicitParams({
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleCode", value = "角色编码", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleName", value = "角色显示名称", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleDesc", value = "描述", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(dataTypeClass = String.class, name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form")
    })
    @PostMapping("/update")
    public ResultBody updateRole(@RequestBody(required = false) BaseRoleForm form) {
        baseRoleService.updateRole(form);
        return ResultBody.ok();
    }


    /**
     * 删除角色
     *
     * @return
     */
    @ApiOperation(value = "删除角色", notes = "删除角色")
    @ApiImplicitParams({
            @ApiImplicitParam(dataTypeClass = String.class, name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "form")
    })
    @PostMapping("/remove")
    public ResultBody removeRole(@RequestBody(required = false) BaseRoleForm form) {
        baseRoleService.removeRole(form.getRoleId());
        return ResultBody.ok();
    }

    /**
     * 角色添加成员
     *
     * @return
     */
    @ApiOperation(value = "角色添加成员", notes = "角色添加成员")
    @PostMapping("/users/add")
    public ResultBody addUserRoles(@RequestBody(required = false) BaseRoleForm form) {
        String[] userIds = StrUtil.splitToArray(form.getUserIds(), ',');
        baseRoleService.saveRoleUsers(form.getRoleId(), userIds);
        return ResultBody.ok();
    }

    /**
     * 查询角色成员
     *
     * @return
     */
    @ApiOperation(value = "查询角色成员", notes = "查询角色成员")
    @PostMapping("/users")
    public ResultBody<List<BaseRoleUser>> getRoleUsers(@RequestBody(required = false) BaseRoleForm form) {
        return ResultBody.callback(() -> baseRoleService.findRoleUsers(form.getRoleId()));
    }

}
