package com.jbm.cluster.auth.service.impl;

import com.jbm.cluster.auth.service.ClientCache;
import com.jbm.cluster.common.security.oauth2.client.JbmOAuth2ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

/**
 * @author: wesley.zhang
 * @date: 2018/11/12 16:26
 * @description:
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ClientDetailsServiceImpl implements ClientDetailsService {

    @Autowired
    private ClientCache clientCache;


    @Autowired
    private JbmOAuth2ClientProperties clientProperties;

    @PostConstruct
    public void loadDefClient() {
        try {
            this.clientCache.getAppClientInfo(clientProperties.getOauth2().get("admin").getClientId());
        } catch (Exception e) {
            log.warn("初始化加载客户端失败", e);
        }
    }

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        ClientDetails details = null;
        try {
            details = clientCache.getAppClientInfo(clientId);
            if (details != null && details.getClientId() != null && details.getAdditionalInformation() != null) {
                String status = details.getAdditionalInformation().getOrDefault("status", "0").toString();
                if (!"1".equals(status)) {
                    log.warn("客户端被禁用");
                    throw new ClientRegistrationException("客户端已被禁用");
                }
            }
            return details;
        } catch (Exception e) {
            log.error("获取客户端登录失败", e);
            throw e;
        }
    }
}
