package com.jbm.cluster.center.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.form.IdsForm;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.basic.BaseAppConfig;
import com.jbm.cluster.center.service.BaseAppConfigService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.security.annotation.PermitAll;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2022-06-27 12:55:11
 */
@Api(tags = "应用配置管理接口")
@RestController
@RequestMapping("/baseAppConfig")
public class BaseAppConfigController extends MasterDataCollection<BaseAppConfig, BaseAppConfigService> {
    @PermitAll
    @ApiOperation("获取应用配置")
    @GetMapping("/getAppConfigByKey")
    public ResultBody<String> getAppConfigByKey(@RequestParam(required = true) String appKey) {
        return ResultBody.callback(() -> {
            BaseAppConfig baseAppConfig = service.getAppConfigByKey(appKey, null);
            if (ObjectUtil.isEmpty(baseAppConfig)) {
                return null;
            }
            if (ObjectUtil.isNotEmpty(LoginHelper.softGetLoginUser())) {
                // 超级管理员账号查询所有数据
                if (LoginHelper.isAdmin()) {
                    return baseAppConfig.getConfigContent();
                }
                baseAppConfig = service.getAppConfigByKey(appKey, LoginHelper.getLoginUser().getCompanyId());
            }
            return baseAppConfig.getConfigContent();
        });
    }


    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseAppConfig>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return super.pageList(pageRequestBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseAppConfig>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.list(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseAppConfig> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseAppConfig> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.save(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseAppConfig>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseAppConfig> mock() {
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
