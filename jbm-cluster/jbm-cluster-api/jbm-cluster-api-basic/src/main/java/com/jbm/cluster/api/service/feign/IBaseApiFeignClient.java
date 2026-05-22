package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.basic.BaseApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

public interface IBaseApiFeignClient {
    @GetMapping(params = "serviceId")
    List<BaseApi> getApiAllList(@RequestParam(required = false) String serviceId);
    @GetMapping(params = {"serviceId", "path"})
    BaseApi findApiByPath(@RequestParam String serviceId, @RequestParam String path);
    @GetMapping("/{apiId}")
    BaseApi getApi(@PathVariable("apiId") Long apiId);
}