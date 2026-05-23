package com.jbm.cluster.auth.service;

import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.satoken.oauth.ClientModelSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 从 base_api_key 解析 OAuth2 Client（第三方 API Key）
 */
@Slf4j
@Service
public class ApiKeyClientModelSource implements ClientModelSource {

    @Autowired
    private BaseApiKeyService baseApiKeyService;

    @Override
    public int getOrder() {
        return -110;
    }

    @Override
    public SaClientModel getClientModel(String clientId) {
        BaseApiKey row;
        try {
            row = baseApiKeyService.getByApiKey(clientId);
        } catch (Exception e) {
            log.debug("未找到 API Key clientId={}", clientId);
            return null;
        }
        if (ObjectUtil.isEmpty(row)) {
            return null;
        }
        return new SaClientModel()
                .setClientId(row.getApiKey())
                .setClientSecret(row.getSecretKey())
                .setAllowUrl("*")
                .setContractScope("all")
                .setIsAutoMode(true);
    }
}
