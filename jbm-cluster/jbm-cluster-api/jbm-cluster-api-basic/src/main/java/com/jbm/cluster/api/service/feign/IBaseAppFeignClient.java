package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface IBaseAppFeignClient {

    @GetMapping("/{appId}")
    BaseApp getApp(@PathVariable("appId") Long appId);

    @GetMapping(params = "apiKey")
    BaseApp getAppByKey(@RequestParam("apiKey") String apiKey);
}