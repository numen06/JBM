package com.jbm.cluster.auth.service;


import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
@Service
public class SaOAuth2TemplateImpl extends SaOAuth2Template {

    // 根据 id 获取 Client 信息
    @Override
    public SaClientModel getClientModel(String clientId) {
        // 此为模拟数据，真实环境需要从数据库查询
        if ("1001".equals(clientId)) {
            return new SaClientModel()
                    .setClientId("10001")
                    .setClientSecret("aaaa-bbbb-cccc-dddd-eeee")
                    .setAllowUrl("*")
                    .setContractScope("userinfo")
                    .setIsAutoMode(true);
        }
        return null;
    }

    @Override
    public AccessTokenModel checkAccessToken(String accessToken) {
        return super.checkAccessToken(accessToken);
    }

    // 根据ClientId 和 LoginId 获取openid
    @Override
    public String getOpenid(String clientId, Object loginId) {
        // 此为模拟数据，真实环境需要从数据库查询
        return Base64.encode(clientId + StrUtil.COLON + loginId);
    }

    @Override
    public void checkScope(String accessToken, String... scopes) {
        super.checkScope(accessToken, scopes);
    }

    /**
     * 生成accessToken
     */
    @Override
    public String randomAccessToken(String clientId, Object loginId, String scope) {
        String token = super.randomAccessToken(clientId, loginId, scope);
        try {
            String tmp = StpUtil.getTokenValue();
            if (StrUtil.isNotEmpty(tmp)) {
                return tmp;
            }
        } catch (Exception e) {
            log.error("获取用户token失败", e);
        }
        return token;
    }
    // -------------- 其它需要重写的函数

}