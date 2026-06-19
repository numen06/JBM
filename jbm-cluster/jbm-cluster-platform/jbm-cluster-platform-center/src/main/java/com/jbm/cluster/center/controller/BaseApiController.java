package com.jbm.cluster.center.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.form.BaseApiForm;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.basic.log.annotation.OperatorLog;
import com.jbm.cluster.common.mysql.service.BaseApiService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统接口资源管理
 */
@Api(tags = "系统接口资源管理")
@RestController
@RequestMapping("/api")
public class BaseApiController extends BaseController {

    @Autowired
    private BaseApiService apiService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @ApiOperation(value = "接口列表")
    @GetMapping
    public ResultBody<?> listApis(
            @ModelAttribute BaseApiForm form,
            @RequestParam(required = false, name = "pageForm.currPage") Integer currPage,
            @RequestParam(required = false, name = "pageForm.pageSize") Integer pageSize) {
        BaseApiForm query = form != null ? form : new BaseApiForm();
        if (query.getServiceId() != null && query.getPath() != null && currPage == null && pageSize == null) {
            return ResultBody.callback(() -> apiService.findApiByPath(query.getServiceId(), query.getPath()));
        }
        if (currPage != null || pageSize != null) {
            return ResultBody.callback(() -> apiService.findListPage(query));
        }
        return ResultBody.callback(() -> apiService.findAllList(query.getServiceId()));
    }

    @ApiOperation(value = "接口服务列表")
    @GetMapping("/services")
    public ResultBody<List<String>> listApiServices() {
        return ResultBody.callback(() -> apiService.findServiceIds());
    }

    @OperatorLog
    @ApiOperation(value = "接口详情")
    @GetMapping("/{apiId}")
    public ResultBody<BaseApi> getApi(@PathVariable Long apiId) {
        return ResultBody.callback(() -> apiService.getApi(apiId));
    }

    @ApiOperation(value = "创建接口")
    @PostMapping
    public ResultBody<BaseApi> createApi(@RequestBody BaseApiForm form) {
        BaseApi baseApi = form;
        apiService.addApi(baseApi);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.callback(() -> baseApi);
    }

    @ApiOperation(value = "更新接口")
    @PutMapping("/{apiId}")
    public ResultBody<Void> updateApi(@PathVariable Long apiId, @RequestBody BaseApiForm form) {
        form.setApiId(apiId);
        apiService.updateApi(form);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除接口")
    @DeleteMapping("/{apiId}")
    public ResultBody<Void> deleteApi(@PathVariable Long apiId) {
        apiService.removeApi(apiId);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    @ApiOperation(value = "批量删除接口")
    @DeleteMapping(params = "ids")
    public ResultBody<Void> batchDeleteApis(@RequestParam String ids) {
        QueryWrapper<BaseApi> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(",")).eq(BaseApi::getIsPersist, 0);
        apiService.remove(wrapper);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    @ApiOperation(value = "批量更新公开状态")
    @PatchMapping(params = {"ids", "open"})
    public ResultBody<Integer> batchPatchOpen(
            @RequestParam List<String> ids,
            @RequestParam Boolean open) {
        return ResultBody.callback(() -> apiService.batchUpdateOpen(ids, open));
    }

    @ApiOperation(value = "批量更新访问日志")
    @PatchMapping(params = {"ids", "accessLog"})
    public ResultBody<Integer> batchPatchAccessLog(
            @RequestParam List<String> ids,
            @RequestParam Boolean accessLog) {
        return ResultBody.callback(() -> apiService.batchUpdateAccessLog(ids, accessLog));
    }

    @ApiOperation(value = "批量更新状态")
    @PatchMapping(params = {"ids", "status"})
    public ResultBody<Void> batchPatchStatus(
            @RequestParam String ids,
            @RequestParam Integer status) {
        Assert.isTrue(status == 0 || status == 1 || status == 2, "status只支持0,1,2");
        QueryWrapper<BaseApi> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(","));
        BaseApi entity = new BaseApi();
        entity.setStatus(status);
        apiService.update(entity, wrapper);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    @ApiOperation(value = "批量更新身份认证")
    @PatchMapping(params = {"ids", "auth"})
    public ResultBody<Void> batchPatchAuth(
            @RequestParam String ids,
            @RequestParam Integer auth) {
        Assert.isTrue(auth == 0 || auth == 1, "auth只支持0,1");
        QueryWrapper<BaseApi> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(",")).eq(BaseApi::getIsPersist, 0);
        BaseApi entity = new BaseApi();
        entity.setIsAuth(auth == 1);
        apiService.update(entity, wrapper);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }
}
