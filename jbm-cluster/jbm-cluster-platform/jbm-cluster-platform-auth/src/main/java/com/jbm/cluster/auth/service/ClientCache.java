package com.jbm.cluster.auth.service;

import com.jbm.cluster.auth.service.feign.BaseAppServiceClient;
import com.jbm.cluster.common.security.OpenClientDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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

    @Cacheable(value = "apps", key = "'client:'+#clientId")
    public ClientDetails getAppClientInfo(String clientId) {
        log.info("客户端调用数据库认证服务:{}", clientId);
        return baseAppRemoteService.getAppClientInfo(clientId).getResult();
    }

    @CachePut(value = {"apps"}, key = "'client:'+#client.clientId")
    public void putAppClientInfo(OpenClientDetails client) {
    }

}
