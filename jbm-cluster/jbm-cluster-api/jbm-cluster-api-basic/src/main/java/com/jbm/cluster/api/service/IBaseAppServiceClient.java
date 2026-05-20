package com.jbm.cluster.api.service;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wesley.zhang
 */
public interface IBaseAppServiceClient {

    @GetMapping("/{appId}")
    ResultBody<BaseApp> getApp(@PathVariable("appId") Long appId);

    @GetMapping(params = "apiKey")
    ResultBody<BaseApp> getAppByKey(@RequestParam("apiKey") String apiKey);
}
