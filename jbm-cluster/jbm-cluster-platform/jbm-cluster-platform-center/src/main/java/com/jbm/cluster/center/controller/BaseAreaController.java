package com.jbm.cluster.center.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.form.IdsForm;
import com.jbm.cluster.core.constant.JbmConstants;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.BaseArea;
import com.jbm.cluster.api.service.IBaseAreaServiceClient;
import com.jbm.cluster.center.service.BaseAreaService;
import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-04-07 21:44:18
 */
@Api(tags = "行政区域开放接口")
@RestController
@RequestMapping("/baseArea")
public class BaseAreaController extends MasterDataCollection<BaseArea, BaseAreaService> implements IBaseAreaServiceClient {

    @ApiOperation("获取地区字典")
    @GetMapping("/getChinaAreaList")
    @PermitAll
    @Override
    public ResultBody<List<BaseArea>> getChinaAreaList() {
        List<BaseArea> result = this.service.getChinaAreaList();
        return ResultBody.success(result, "获取地区字典成功");
    }


    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseArea>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return super.pageList(pageRequestBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseArea>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.list(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseArea> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseArea> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.save(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseArea>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseArea> mock() {
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
