package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.cluster.api.service.IExtendFormDefinitionServiceClient;
import com.jbm.cluster.common.mysql.service.ExtendFormDefinitionService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "扩展字段表单定义")
@RestController
@RequestMapping("/extend-field/forms")
public class ExtendFormDefinitionController implements IExtendFormDefinitionServiceClient {

    @Autowired
    private ExtendFormDefinitionService extendFormDefinitionService;

    @Override
    @ApiOperation("分页查询表单定义列表")
    public ResultBody<DataPaging<ExtendFormDefinition>> listFromDb(
            @RequestParam(value = "pageForm.currPage", required = false) Integer currPage,
            @RequestParam(value = "pageForm.pageSize", required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        PageForm pageForm = new PageForm();
        pageForm.setCurrPage(currPage);
        pageForm.setPageSize(pageSize);
        return ResultBody.ok(extendFormDefinitionService.pageByTenant(keyword, pageForm));
    }

    @Override
    @ApiOperation("新建或更新并发布")
    public ResultBody<ExtendFormDefinition> save(String formCode, SaveExtendFormRequest request) {
        return ResultBody.ok(extendFormDefinitionService.saveAndPublish(formCode, request));
    }

    @Override
    @ApiOperation("更新并发布")
    public ResultBody<ExtendFormDefinition> update(String formCode, SaveExtendFormRequest request) {
        return ResultBody.ok(extendFormDefinitionService.saveAndPublish(formCode, request));
    }

    @Override
    @ApiOperation("从库重新发布到 Redis")
    public ResultBody<Boolean> publish(String formCode) {
        extendFormDefinitionService.publishToRedis(formCode);
        return ResultBody.ok(true);
    }

    @Override
    @ApiOperation("从库读取定义")
    public ResultBody<ExtendFormDefinition> getFromDb(String formCode) {
        return ResultBody.ok(extendFormDefinitionService.getByFormCode(formCode));
    }

    @Override
    @ApiOperation("从 Redis 读取当前字段定义")
    public ResultBody<List<FieldDefinition>> listFromRedis(String formCode) {
        return ResultBody.ok(extendFormDefinitionService.listFromRedis(formCode));
    }
}
