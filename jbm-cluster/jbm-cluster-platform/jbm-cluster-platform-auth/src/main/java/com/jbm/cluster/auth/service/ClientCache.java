package com.jbm.cluster.auth.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jbm.cluster.auth.service.feign.BaseAppServiceClient;
import com.jbm.cluster.common.configuration.JbmClusterProperties;
import com.jbm.cluster.common.security.OpenClientDetails;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class ClientCache {


    @Resource
    private BaseAppServiceClient baseAppRemoteService;

    @Autowired
    private JbmClusterProperties jbmClusterProperties;

    @Cacheable(value = "apps", key = "'client:'+#clientId")
    public ClientDetails getAppClientInfo(String clientId) {
        String url = StrUtil.format("http://{}/app/client/{}/info", jbmClusterProperties.getAdminServerAddr(), clientId);
        log.info("客户端调用数据库认证服务:{},URL:{}", clientId, url);
        HttpResponse httpResponse = HttpRequest.get(url).execute();
        ResultBody<OpenClientDetails> result = JSON.parseObject(httpResponse.body(), new TypeReference<ResultBody<OpenClientDetails>>() {
        });
//        return baseAppRemoteService.getAppClientInfo(clientId).getResult();
        return result.getResult();
    }

    @CachePut(value = {"apps"}, key = "'client:'+#client.clientId")
    public void putAppClientInfo(OpenClientDetails client) {
    }

}
