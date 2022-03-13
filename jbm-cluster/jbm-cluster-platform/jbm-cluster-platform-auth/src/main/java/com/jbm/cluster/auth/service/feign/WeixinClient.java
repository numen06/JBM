package com.jbm.cluster.auth.service.feign;

import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author fyk
 */
@FeignClient(name = WeixinClient.SERVICE_ID)
public interface WeixinClient {
    /**
     * eureka service name
     */
    String SERVICE_ID = "jbm-cluster-platform-weixin";

    @RequestMapping(value = "/user/{appid}/login", method = RequestMethod.GET)
    ResultBody<String> login(@PathVariable(value = "appid") String appid, @RequestParam("code") String code);


}
