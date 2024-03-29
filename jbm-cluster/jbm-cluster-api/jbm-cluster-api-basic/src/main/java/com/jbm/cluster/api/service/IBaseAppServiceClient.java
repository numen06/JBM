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

    /**
     * 获取应用基础信息
     *
     * @param appId 应用Id
     * @return
     */
    @GetMapping("/{appId}/info")
    ResultBody<BaseApp> getApp(@PathVariable("appId") Long appId);

    @GetMapping("/getAppByKey")
    ResultBody<BaseApp> getAppByKey(@RequestParam(name = "appKey", required = true) String appKey);

//    /**
//     * 获取应用开发配置信息
//     * @param clientId
//     * @return
//     */
//    @GetMapping("/app/client/{clientId}/info")
//    ResultBody<OpenClientDetails> getAppClientInfo(@PathVariable("clientId") String clientId);
}
