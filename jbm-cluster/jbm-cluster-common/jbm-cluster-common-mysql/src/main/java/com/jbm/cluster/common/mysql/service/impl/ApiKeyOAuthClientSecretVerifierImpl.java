package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.OAuthClientSecretVerifier;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.common.mysql.mapper.BaseApiKeyMapper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 校验 base_api_key 表中的 OAuth client_secret
 */
@Component
@Order(-10)
public class ApiKeyOAuthClientSecretVerifierImpl implements OAuthClientSecretVerifier {

    @Autowired
    private BaseApiKeyMapper baseApiKeyMapper;

    @Override
    public boolean verify(String clientId, String clientSecret) {
        if (StrUtil.hasBlank(clientId, clientSecret)) {
            return false;
        }
        QueryWrapper<BaseApiKey> q = new QueryWrapper<>();
        q.lambda().eq(BaseApiKey::getApiKey, clientId.trim());
        List<BaseApiKey> rows = baseApiKeyMapper.selectList(q);
        BaseApiKey row = CollUtil.getFirst(rows);
        if (row == null || StrUtil.isBlank(row.getSecretKey())) {
            return false;
        }
        String stored = row.getSecretKey();
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$")) {
            return SecurityUtils.matchesPassword(clientSecret, stored);
        }
        return stored.equals(clientSecret);
    }
}
