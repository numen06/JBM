package com.jbm.cluster.auth.service.impl;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Maps;
import com.jbm.cluster.auth.service.feign.BaseAppServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author: wesley.zhang
 * @date: 2018/11/12 16:26
 * @description:
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ClientDetailsServiceImpl implements ClientDetailsService {

    @Resource
    private BaseAppServiceClient baseAppRemoteService;

    private Cache<String, ClientDetails> clientDetailsMap = CacheUtil.newTimedCache(1000 * 60 * 60 * 24);

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        ClientDetails details = null;
        if (clientDetailsMap.containsKey(clientId)) {
            details = clientDetailsMap.get(clientId);
            log.info("客户端调用缓存认证服务:{}", clientId);
        } else {
            details = baseAppRemoteService.getAppClientInfo(clientId).getResult();
            log.info("客户端调用数据库认证服务:{}", clientId);
        }
        if (details != null && details.getClientId() != null && details.getAdditionalInformation() != null) {
            String status = details.getAdditionalInformation().getOrDefault("status", "0").toString();
            if (!"1".equals(status)) {
                log.warn("客户端被禁用");
                throw new ClientRegistrationException("客户端已被禁用");
            }
        }
        clientDetailsMap.put(clientId, details);
        return details;
    }
}
