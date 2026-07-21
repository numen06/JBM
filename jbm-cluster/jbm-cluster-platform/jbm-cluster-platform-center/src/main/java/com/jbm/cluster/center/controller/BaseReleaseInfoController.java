package com.jbm.cluster.center.controller;

import java.util.List;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.form.IdsForm;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.BaseReleaseInfo;
import com.jbm.cluster.center.service.BaseReleaseInfoService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2021-08-25 10:49:30
 */
@Api(tags = "版本发布管理")
@RestController
@RequestMapping("/baseReleaseInfo")
public class BaseReleaseInfoController extends MasterDataCollection<BaseReleaseInfo, BaseReleaseInfoService> {

    @ApiOperation(value = "查询最后一个版本信息", notes = "查询最后一个版本信息")
    @PostMapping("/findLastVersionInfo")
    public ResultBody<BaseReleaseInfo> findLastVersionInfo(@RequestBody(required = false) BaseReleaseInfo releaseInfo) {
        try {
            BaseReleaseInfo reslut = service.findLastVersionInfo(releaseInfo);
            return ResultBody.success(reslut, "查询最后一个版本成功");
        } catch (Exception e) {
            return ResultBody.error(e);
        }
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseReleaseInfo>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return super.pageList(pageRequestBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseReleaseInfo>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.list(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseReleaseInfo> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseReleaseInfo> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.save(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseReleaseInfo>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseReleaseInfo> mock() {
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
