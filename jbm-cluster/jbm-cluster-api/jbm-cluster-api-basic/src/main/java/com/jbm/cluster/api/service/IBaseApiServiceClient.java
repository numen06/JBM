package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.core.annotation.InnerAuth;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface IBaseApiServiceClient {
    @InnerAuth
    @GetMapping("/all")
    ResultBody<List<BaseApi>> getApiAllList(String serviceId);

    @InnerAuth
    @ApiOperation(value = "根据路径查询API")
    @GetMapping("/findApiByPath")
    ResultBody<BaseApi> findApiByPath(@RequestParam("serviceId") String serviceId, @RequestParam("path") String path);

    @InnerAuth
    @GetMapping("/{apiId}/info")
    ResultBody<BaseApi> getApi(@PathVariable("apiId") Long apiId);
}
