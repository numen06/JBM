package com.jbm.cluster.center.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import io.swagger.annotations.ApiOperation;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.form.IdsForm;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.BaseOpenUser;
import com.jbm.cluster.center.service.BaseOpenUserService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-19 14:01:27
 */
@Api(tags = "开放用户信息开放接口")
@RestController
@RequestMapping("/baseOpenUser")
public class BaseOpenUserController extends MasterDataCollection<BaseOpenUser, BaseOpenUserService> {

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseOpenUser>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return super.pageList(pageRequestBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseOpenUser>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.list(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseOpenUser> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseOpenUser> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.save(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseOpenUser>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseOpenUser> mock() {
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
