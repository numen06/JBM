package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.constants.OAuthClientSecretVerifier;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.mysql.mapper.BaseAppMapper;
import com.jbm.cluster.common.satoken.utils.AppSecretCodec;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 校验 OAuth client_secret，支持 BCrypt 存储的 secretKey。
 */
@Component
public class OAuthClientSecretVerifierImpl implements OAuthClientSecretVerifier {

    @Autowired
    private BaseAppMapper baseAppMapper;

    @Override
    public boolean verify(String clientId, String clientSecret) {
        if (StrUtil.hasBlank(clientId, clientSecret)) {
            return false;
        }
        QueryWrapper<BaseApp> q = new QueryWrapper<>();
        q.lambda().eq(BaseApp::getApiKey, clientId.trim());
        List<BaseApp> apps = baseAppMapper.selectList(q);
        BaseApp app = CollUtil.getFirst(apps);
        if (app == null || StrUtil.isBlank(app.getSecretKey())) {
            return false;
        }
        String stored = app.getSecretKey();
        return AppSecretCodec.verify(clientSecret, stored);
    }
}
