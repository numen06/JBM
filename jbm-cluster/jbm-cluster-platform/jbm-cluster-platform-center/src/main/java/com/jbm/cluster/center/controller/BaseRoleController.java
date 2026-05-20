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
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统角色管理
 */
@Api(tags = "系统角色管理")
@RestController
@RequestMapping("/role")
public class BaseRoleController extends BaseController {

    @Autowired
    private BaseRoleService baseRoleService;

    @ApiOperation(value = "角色分页列表")
    @GetMapping
    public ResultBody<DataPaging<BaseRole>> listRoles(@ModelAttribute BaseRoleForm form) {
        return ResultBody.callback(() -> baseRoleService.findListPage(form != null ? form : new BaseRoleForm()));
    }

    @ApiOperation(value = "全部角色")
    @GetMapping("/all")
    public ResultBody<List<BaseRole>> listAllRoles() {
        return ResultBody.callback(() -> baseRoleService.findAllList());
    }

    @ApiOperation(value = "角色详情")
    @GetMapping("/{roleId}")
    public ResultBody<BaseRole> getRole(@PathVariable Long roleId) {
        return ResultBody.callback(() -> baseRoleService.getRole(roleId));
    }

    @ApiOperation(value = "创建角色")
    @PostMapping
    public ResultBody<Long> createRole(@RequestBody BaseRoleForm form) {
        return ResultBody.callback(() -> {
            BaseRole result = baseRoleService.addRole(form);
            return result != null ? result.getRoleId() : null;
        });
    }

    @ApiOperation(value = "更新角色")
    @PutMapping("/{roleId}")
    public ResultBody<Void> updateRole(@PathVariable Long roleId, @RequestBody BaseRoleForm form) {
        form.setRoleId(roleId);
        baseRoleService.updateRole(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除角色")
    @DeleteMapping("/{roleId}")
    public ResultBody<Void> deleteRole(@PathVariable Long roleId) {
        baseRoleService.removeRole(roleId);
        return ResultBody.ok();
    }

    @ApiOperation(value = "角色成员")
    @GetMapping("/{roleId}/users")
    public ResultBody<List<BaseRoleUser>> listRoleUsers(@PathVariable Long roleId) {
        return ResultBody.callback(() -> baseRoleService.findRoleUsers(roleId));
    }

    @ApiOperation(value = "设置角色成员")
    @PutMapping("/{roleId}/users")
    public ResultBody<Void> putRoleUsers(@PathVariable Long roleId, @RequestBody BaseRoleForm form) {
        String[] userIds = StrUtil.splitToArray(form.getUserIds(), ',');
        baseRoleService.saveRoleUsers(roleId, userIds);
        return ResultBody.ok();
    }
}
