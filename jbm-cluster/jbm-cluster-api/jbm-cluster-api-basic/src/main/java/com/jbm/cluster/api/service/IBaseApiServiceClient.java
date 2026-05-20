package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author wesley.zhang
 */
public interface IBaseApiServiceClient {

    @GetMapping(params = "serviceId")
    ResultBody<List<BaseApi>> getApiAllList(@RequestParam(required = false) String serviceId);

    @GetMapping(params = {"serviceId", "path"})
    ResultBody<BaseApi> findApiByPath(
            @RequestParam String serviceId,
            @RequestParam String path);

    @GetMapping("/{apiId}")
    ResultBody<BaseApi> getApi(@PathVariable("apiId") Long apiId);
}
