package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.center.ExtendFormDefinition;
import com.jbm.cluster.api.form.center.SaveExtendFormRequest;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 扩展字段表单定义管理（Center 提供，其它服务 Feign 调用）。
 */
public interface IExtendFormDefinitionServiceClient {

    @ApiOperation("分页查询表单定义列表")
    @GetMapping
    ResultBody<DataPaging<ExtendFormDefinition>> listFromDb(
            @RequestParam(value = "pageForm.currPage", required = false) Integer currPage,
            @RequestParam(value = "pageForm.pageSize", required = false) Integer pageSize,
            @RequestParam(required = false) String keyword);

    @ApiOperation("新建或更新并发布")
    @PostMapping("/{formCode}")
    ResultBody<ExtendFormDefinition> save(
            @PathVariable("formCode") String formCode,
            @RequestBody SaveExtendFormRequest request);

    @ApiOperation("更新并发布")
    @PutMapping("/{formCode}")
    ResultBody<ExtendFormDefinition> update(
            @PathVariable("formCode") String formCode,
            @RequestBody SaveExtendFormRequest request);

    @ApiOperation("从库重新发布到 Redis")
    @PostMapping("/{formCode}/publish")
    ResultBody<Boolean> publish(@PathVariable("formCode") String formCode);

    @ApiOperation("从库读取定义")
    @GetMapping("/{formCode}")
    ResultBody<ExtendFormDefinition> getFromDb(@PathVariable("formCode") String formCode);

    @ApiOperation("从 Redis 读取当前字段定义")
    @GetMapping("/{formCode}/definitions")
    ResultBody<List<FieldDefinition>> listFromRedis(@PathVariable("formCode") String formCode);
}
