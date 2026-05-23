package com.jbm.cluster.platform.gateway.security;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.platform.gateway.config.CenterFeignClients;
import com.jbm.cluster.api.service.feign.client.BaseAppServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ApiClientConfigProvider {

    private final LoadingCache<String, String> publicKeyCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(this::loadPublicKey);

    private final LoadingCache<String, BaseApiKey> apiKeyCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(this::loadApiKey);

    @Autowired
    private BaseAppServiceClient baseAppServiceClient;
    @Autowired
    private CenterFeignClients centerFeignClients;

    public String getPublicKey(String appId) {
        if (StrUtil.isBlank(appId)) {
            return null;
        }
        try {
            return publicKeyCache.get(appId.trim());
        } catch (Exception e) {
            log.debug("[ApiClientConfigProvider] load publicKey failed appId={}: {}", appId, e.getMessage());
            return null;
        }
    }

    public BaseApiKey resolveApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return null;
        }
        try {
            return apiKeyCache.get(apiKey.trim());
        } catch (Exception e) {
            log.debug("[ApiClientConfigProvider] load apiKey failed: {}", e.getMessage());
            return null;
        }
    }

    private String loadPublicKey(String appId) {
        BaseApiKey apiKeyRow = loadApiKey(appId);
        if (apiKeyRow != null && StrUtil.isNotBlank(apiKeyRow.getPublicKey())) {
            return apiKeyRow.getPublicKey();
        }
        BaseApp app = baseAppServiceClient.getAppByKey(appId);
        if (app == null || StrUtil.isBlank(app.getPublicKey())) {
            return null;
        }
        return app.getPublicKey();
    }

    private BaseApiKey loadApiKey(String apiKey) {
        try {
            return centerFeignClients.apiKey().getByApiKey(apiKey);
        } catch (Exception e) {
            return null;
        }
    }
}
