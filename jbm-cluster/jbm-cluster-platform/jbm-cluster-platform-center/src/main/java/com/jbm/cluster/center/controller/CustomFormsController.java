package com.jbm.cluster.center.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.center.CustomForms;
import com.jbm.cluster.api.form.center.CustomFormsForm;
import com.jbm.cluster.api.result.CustomFormsResult;
import com.jbm.cluster.center.service.CustomFormsService;
import com.jbm.cluster.core.constant.JbmConstants;
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
 * @Create: 2025-07-23 16:08:36
 */
@Api(tags = "自定义表单开放接口")
@RestController
@RequestMapping("/customForms")
@SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
public class CustomFormsController extends MasterDataCollection<CustomForms, CustomFormsService> {

    @Autowired
    private CustomFormsService customFormsService;

    @ApiOperation("保存自定义表单")
    @PostMapping("/saveData")
    public ResultBody<CustomForms> saveData(@RequestBody @Valid CustomFormsForm form) {
        return ResultBody.callback(() -> customFormsService.saveData(form));
    }

    @ApiOperation("获取自定义表单详情")
    @PostMapping("/getDetail")
    public ResultBody<CustomFormsResult> getDetail(@RequestBody(required = false) CustomFormsForm form) {
        return ResultBody.callback(() -> customFormsService.getDetail(form));
    }
}
