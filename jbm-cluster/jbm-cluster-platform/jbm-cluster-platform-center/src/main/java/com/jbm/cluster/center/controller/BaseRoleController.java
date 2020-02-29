package com.jbm.cluster.center.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.model.entity.BaseRole;
import com.jbm.cluster.api.model.entity.BaseRoleUser;
import com.jbm.cluster.center.service.BaseRoleService;
import com.jbm.framework.masterdata.controller.IMasterDataController;
import com.jbm.framework.masterdata.usage.entity.MasterDataCodeEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Api(tags = "系统角色管理")
@RestController
@RequestMapping("/role")
public class BaseRoleController extends MasterDataCollection<BaseRole, BaseRoleService> {
    @Autowired
    private BaseRoleService baseRoleService;

    /**
     * 获取分页角色列表
     *
     * @return
     */
    @ApiOperation(value = "获取分页角色列表", notes = "获取分页角色列表")
    @PostMapping("")
    public ResultBody<DataPaging<BaseRole>> getRoleListPage(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return ResultBody.ok().data(baseRoleService.findListPage(pageRequestBody));
    }

    /**
     * 获取所有角色列表
     *
     * @return
     */
    @ApiOperation(value = "获取所有角色列表", notes = "获取所有角色列表")
    @PostMapping("/all")
    public ResultBody<List<BaseRole>> getRoleAllList() {
        return ResultBody.ok().data(baseRoleService.findAllList());
    }

    /**
     * 获取角色详情
     *
     * @param roleId
     * @return
     */
    @ApiOperation(value = "获取角色详情", notes = "获取角色详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "path")
    })
    @PostMapping("/{roleId}/info")
    public ResultBody<BaseRole> getRole(@PathVariable(value = "roleId") Long roleId) {
        BaseRole result = baseRoleService.getRole(roleId);
        return ResultBody.ok().data(result);
    }

    /**
     * 添加角色
     *
     * @return
     */
    @ApiOperation(value = "添加角色", notes = "添加角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleCode", value = "角色编码", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "roleName", value = "角色显示名称", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "roleDesc", value = "描述", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form")
    })
    @PostMapping("/add")
    public ResultBody<Long> addRole(@RequestBody(required = false) PageRequestBody pageRequestBody) {
//        BaseRole role = new BaseRole();
//        role.setRoleCode(roleCode);
//        role.setRoleName(roleName);
//        role.setStatus(status);
//        role.setRoleDesc(roleDesc);
        Long roleId = null;
        BaseRole result = baseRoleService.addRole(pageRequestBody.tryGet(BaseRole.class));
        if (result != null) {
            roleId = result.getRoleId();
        }
        return ResultBody.ok().data(roleId);
    }

    /**
     * 编辑角色
     *
     * @return
     */
    @ApiOperation(value = "编辑角色", notes = "编辑角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "roleCode", value = "角色编码", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "roleName", value = "角色显示名称", defaultValue = "", required = true, paramType = "form"),
            @ApiImplicitParam(name = "roleDesc", value = "描述", defaultValue = "", required = false, paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form")
    })
    @PostMapping("/update")
    public ResultBody updateRole(
//            @RequestParam(value = "roleId") Long roleId,
//            @RequestParam(value = "roleCode") String roleCode,
//            @RequestParam(value = "roleName") String roleName,
//            @RequestParam(value = "roleDesc", required = false) String roleDesc,
//            @RequestParam(value = "status", defaultValue = "1", required = false) Integer status
            @RequestBody(required = false) PageRequestBody pageRequestBody
    ) {
//        BaseRole role = new BaseRole();
//        role.setRoleId(roleId);
//        role.setRoleCode(roleCode);
//        role.setRoleName(roleName);
//        role.setStatus(status);
//        role.setRoleDesc(roleDesc);
        baseRoleService.updateRole(pageRequestBody.tryGet(BaseRole.class));
        return ResultBody.ok();
    }


    /**
     * 删除角色
     *
     * @return
     */
    @ApiOperation(value = "删除角色", notes = "删除角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "form")
    })
    @PostMapping("/remove")
    public ResultBody removeRole(@RequestBody(required = false) PageRequestBody pageRequestBody
//            @RequestParam(value = "roleId") Long roleId
    ) {
        baseRoleService.removeRole(pageRequestBody.tryGet(BaseRole.class).getRoleId());
        return ResultBody.ok();
    }

    /**
     * 角色添加成员
     *
     * @return
     */
    @ApiOperation(value = "角色添加成员", notes = "角色添加成员")
    @PostMapping("/users/add")
    public ResultBody addUserRoles(
//            @RequestParam(value = "roleId") Long roleId,
//            @RequestParam(value = "userIds", required = false) String userIds
            @RequestBody(required = false) PageRequestBody pageRequestBody
    ) {
        BaseRole role = pageRequestBody.tryGet(BaseRole.class);
        String[] userIds = StrUtil.splitToArray(pageRequestBody.getString("userIds"), ',');
        baseRoleService.saveRoleUsers(role.getRoleId(), userIds);
        return ResultBody.ok();
    }

    /**
     * 查询角色成员
     *
     * @param roleId
     * @return
     */
    @ApiOperation(value = "查询角色成员", notes = "查询角色成员")
    @PostMapping("/users")
    public ResultBody<List<BaseRoleUser>> getRoleUsers(
//            @RequestParam(value = "roleId") Long roleId
            @RequestBody(required = false) PageRequestBody pageRequestBody
    ) {
        BaseRole role = pageRequestBody.tryGet(BaseRole.class);
        return ResultBody.ok().data(baseRoleService.findRoleUsers(role.getRoleId()));
    }

}
