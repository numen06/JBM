package com.jbm.cluster.center.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.form.IdsForm;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseRoleUser;
import com.jbm.cluster.center.service.BaseRoleService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
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
public class BaseRoleController extends MasterDataCollection<BaseRole, BaseRoleService> {
    @Autowired
    private BaseRoleService baseRoleService;

    /**
     * 获取分页角色列表
     *
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页角色列表", notes = "获取分页角色列表")
    @PostMapping("")
    public ResultBody<DataPaging<BaseRole>> getRoleListPage(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return ResultBody.callback(() -> baseRoleService.findListPage(pageRequestBody));
    }

    /**
     * 获取所有角色列表
     *
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取角色详情", notes = "获取角色详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", defaultValue = "", required = true, paramType = "path")
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
        return ResultBody.callback(() -> {
            Long roleId = null;
            BaseRole result = baseRoleService.addRole(pageRequestBody.tryGet(BaseRole.class));
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
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
     * @return
     */
    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "查询角色成员", notes = "查询角色成员")
    @PostMapping("/users")
    public ResultBody<List<BaseRoleUser>> getRoleUsers(
//            @RequestParam(value = "roleId") Long roleId
            @RequestBody(required = false) PageRequestBody pageRequestBody
    ) {
        BaseRole role = pageRequestBody.tryGet(BaseRole.class);
        return ResultBody.callback(() -> baseRoleService.findRoleUsers(role.getRoleId()));
    }


    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseRole>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return super.pageList(pageRequestBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseRole>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.list(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseRole> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseRole> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.save(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseRole>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseRole> mock() {
        return super.mock();
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.remove(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) IdsForm idsForm) {
        return super.deleteByIds(idsForm);
    }

}
