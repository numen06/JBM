package com.jbm.cluster.center.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.service.IBaseApiServiceClient;
import com.jbm.cluster.center.service.BaseApiService;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.basic.log.annotation.OperatorLog;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author wesley.zhang
 */
@Api(tags = "系统接口资源管理")
@RestController
@RequestMapping("/api")
public class BaseApiController extends MasterDataCollection<BaseApi, BaseApiService> implements IBaseApiServiceClient {
    @Autowired
    private BaseApiService apiService;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

//    /**
//     * 获取分页接口列表
//     *
//     * @return
//     */
//    @ApiOperation(value = "获取分页接口列表", notes = "获取分页接口列表")
//    @GetMapping(value = "/api")
//    public ResultBody<IPage<BaseApi>> getApiList(@RequestParam(required = false) Map map) {
//        return ResultBody.ok().data(apiService.findListPage(PageRequestBody.from(map)));
//    }


    /**
     * 获取所有接口列表
     *
     * @return
     */
    @ApiOperation(value = "获取单个应用的接口列表")
    @GetMapping("/all")
    @Override
    public ResultBody<List<BaseApi>> getApiAllList(String serviceId) {
        return ResultBody.ok().data(apiService.findAllList(serviceId));
    }

    @ApiOperation(value = "根据路径查询API")
    @GetMapping("/findApiByPath")
    @Override
    public ResultBody<BaseApi> findApiByPath(String serviceId, String path) {
        return ResultBody.callback(new Supplier<BaseApi>() {
            @Override
            public BaseApi get() {
                return apiService.findApiByPath(serviceId, path);
            }
        });
    }

    /**
     * 获取接口资源
     *
     * @param apiId
     * @return
     */
    @Override
    @OperatorLog
    @ApiOperation(value = "获取接口资源", notes = "获取接口资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "apiId", required = true, value = "ApiId", paramType = "path"),
    })
    @GetMapping("/{apiId}/info")
    public ResultBody<BaseApi> getApi(@PathVariable("apiId") Long apiId) {
        return ResultBody.ok().data(apiService.getApi(apiId));
    }

    /**
     * 添加接口资源
     *
     * @param baseApi 接口编码
     * @return
     */
    @ApiOperation(value = "添加接口资源", notes = "添加接口资源")
    @PostMapping("/add")
    public ResultBody<BaseApi> addApi(BaseApi baseApi) {
        apiService.addApi(baseApi);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok().data(baseApi);
    }

    /**
     * 编辑接口资源
     *
     * @param baseApi 接口ID
     * @return
     */
    @ApiOperation(value = "编辑接口资源", notes = "编辑接口资源")
    @PostMapping("/update")
    public ResultBody updateApi(BaseApi baseApi) {
//        api.setApiId(apiId);
//        api.setApiCode(apiCode);
//        api.setApiName(apiName);
//        api.setApiCategory(apiCategory);
//        api.setServiceId(serviceId);
//        api.setPath(path);
//        api.setStatus(status);
//        api.setPriority(priority);
//        api.setApiDesc(apiDesc);
//        api.setIsAuth(isAuth);
//        api.setIsOpen(isOpen);
        apiService.updateApi(baseApi);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 移除接口资源
     *
     * @param apiId
     * @return
     */
    @ApiOperation(value = "移除接口资源", notes = "移除接口资源")
    @PostMapping("/remove")
    public ResultBody removeApi(@RequestParam("apiId") Long apiId) {
        apiService.removeApi(apiId);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 批量删除数据
     *
     * @return
     */
    @ApiOperation(value = "批量删除数据", notes = "批量删除数据")
    @PostMapping("/batch/remove")
    public ResultBody batchRemove(@RequestParam(value = "ids") String ids) {
        QueryWrapper<BaseApi> wrapper = new QueryWrapper();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(",")).eq(BaseApi::getIsPersist, 0);
        apiService.remove(wrapper);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }


    /**
     * 批量修改公开状态
     *
     * @return
     */
    @ApiOperation(value = "批量修改公开状态", notes = "批量修改公开状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", required = true, value = "多个用,号隔开", paramType = "form"),
            @ApiImplicitParam(name = "open", required = true, value = "是否公开访问:0-否 1-是", paramType = "form")
    })
    @PostMapping("/batch/update/open")
    public ResultBody batchUpdateOpen(@RequestParam(value = "ids") String ids,
                                      @RequestParam(value = "open") Integer open
    ) {
        Assert.isTrue((open.intValue() != 1 || open.intValue() != 0), "isOpen只支持0,1");
        QueryWrapper<BaseApi> wrapper = new QueryWrapper();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(","));
        BaseApi entity = new BaseApi();
        entity.setIsOpen(open);
        apiService.update(entity, wrapper);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    /**
     * 批量修改状态
     *
     * @return
     */
    @ApiOperation(value = "批量修改状态", notes = "批量修改状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", required = true, value = "多个用,号隔开", paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, value = "接口状态:0-禁用 1-启用", paramType = "form")
    })
    @PostMapping("/batch/update/status")
    public ResultBody batchUpdateStatus(
            @RequestParam(value = "ids") String ids,
            @RequestParam(value = "status") Integer status
    ) {
        Assert.isTrue((status.intValue() != 0 || status.intValue() != 1 || status.intValue() != 2), "status只支持0,1,2");
        QueryWrapper<BaseApi> wrapper = new QueryWrapper();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(","));
        BaseApi entity = new BaseApi();
        entity.setStatus(status);
        apiService.update(entity, wrapper);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }

    /**
     * 批量修改身份认证
     *
     * @return
     */
    @ApiOperation(value = "批量修改身份认证", notes = "批量修改身份认证")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", required = true, value = "多个用,号隔开", paramType = "form"),
            @ApiImplicitParam(name = "auth", required = true, value = "是否身份认证:0-否 1-是", paramType = "form")
    })
    @PostMapping("/batch/update/auth")
    public ResultBody batchUpdateAuth(
            @RequestParam(value = "ids") String ids,
            @RequestParam(value = "auth") Integer auth
    ) {
        Assert.isTrue((auth.intValue() != 0 || auth.intValue() != 1), "auth只支持0,1");
        QueryWrapper<BaseApi> wrapper = new QueryWrapper();
        wrapper.lambda().in(BaseApi::getApiId, ids.split(",")).eq(BaseApi::getIsPersist, 0);
        BaseApi entity = new BaseApi();
        entity.setStatus(auth);
        apiService.update(entity, wrapper);
        // 刷新网关
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }
}
