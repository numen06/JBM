package com.jbm.cluster.center.controller;

import cn.hutool.core.lang.Assert;
import com.jbm.cluster.api.constants.center.DataSourceType;
import com.jbm.cluster.api.entitys.center.DataSourceManagement;
import com.jbm.cluster.api.form.center.DataSourceManagementForm;
import com.jbm.cluster.common.mysql.service.DataSourceManagementService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-24 10:58:54
 */
@Api(tags = "数据源管理开放接口")
@RestController
@RequestMapping("/dataSourceManagement")
public class DataSourceManagementController extends MasterDataCollection<DataSourceManagement, DataSourceManagementService> {

    @Autowired
    private DataSourceManagementService dataSourceManagementService;

    @ApiOperation("保存数据源管理")
    @PostMapping("/saveData")
    public ResultBody<DataSourceManagement> saveData(@RequestBody @Valid DataSourceManagementForm form) {
        if(DataSourceType.http.equals(form.getDataSourceType())){
            Assert.notNull(form.getUrl(), "URL地址不能为空");
            Assert.notNull(form.getRequestMethod(), "请求方式不能为空");
            Assert.notNull(form.getRequestHeader(), "请求头不能为空");
        }
        return ResultBody.callback(() -> dataSourceManagementService.saveData(form));
    }

}
