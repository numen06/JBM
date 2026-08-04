package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.center.service.BaseOrgService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.form.ObjectIdsForm;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import com.jbm.framework.usage.form.EntityPageSearchForm;
import com.jbm.framework.usage.form.EntityRequestForm;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-03-24 03:28:09
 */
@Api(tags = "组织结构管理")
@RestController
@RequestMapping("/baseOrg")
public class BaseOrgController extends MultiPlatformCollection<BaseOrg, BaseOrgService> {

    @ApiOperation(value = "获取顶层公司", notes = "获取顶层公司")
    @PostMapping("/findTopCompany")
    public ResultBody<BaseOrg> findTopCompany(@RequestBody BaseOrg baseOrg) {
        return ResultBody.callback(() -> this.service.findTopCompany(baseOrg));
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取下级公司", notes = "获取下级公司")
    @PostMapping("/findRelegationCompany")
    public ResultBody<List<BaseOrg>> findRelegationCompany(@RequestBody BaseOrg baseOrg) {
        return ResultBody.callback(() -> this.service.findRelegationCompany(baseOrg));
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取组织信息", notes = "获取组织信息")
    @PostMapping("/getBaseOrg")
    public ResultBody<BaseOrg> getBaseOrg(@RequestBody BaseOrg baseOrg) {
        return ResultBody.callback(() -> this.service.getBaseOrg(baseOrg));
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseOrg>> pageList(@RequestBody(required = false) EntityPageSearchForm<BaseOrg> entityPageSearchForm) {
        return super.pageList(entityPageSearchForm);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseOrg>> list(@RequestBody(required = false) EntityRequestForm<BaseOrg> entityRequestForm) {
        return super.list(entityRequestForm);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseOrg> model(@RequestBody(required = false) EntityRequestForm<BaseOrg> entityRequestForm) {
        return super.model(entityRequestForm);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseOrg> save(@RequestBody(required = false) EntityRequestForm<BaseOrg> entityRequestForm) {
        return super.save(entityRequestForm);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseOrg>> saveBatch(@RequestBody(required = false) EntityRequestForm<BaseOrg> entityRequestForm) {
        return super.saveBatch(entityRequestForm);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseOrg> mock() {
        return super.mock();
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) EntityRequestForm<BaseOrg> entityRequestForm) {
        return super.remove(entityRequestForm);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) ObjectIdsForm objectIdsForm) {
        return super.deleteByIds(objectIdsForm);
    }
}
