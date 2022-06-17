package com.jbm.cluster.auth.service;


import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
@Service
public class SaOAuth2TemplateImpl extends SaOAuth2Template {

    @Autowired
    private BaseAppPreprocessing baseAppPreprocessing;

    // 根据 id 获取 Client 信息
    @Override
    public SaClientModel getClientModel(String clientId) {
        BaseApp baseApp = baseAppPreprocessing.getAppByKey(clientId);
        return new SaClientModel()
                .setClientId(baseApp.getApiKey())
                .setClientSecret(baseApp.getSecretKey())
                .setAllowUrl("*")
                .setContractScope("*")
                .setIsAutoMode(true);
    }

    @Override
    public AccessTokenModel checkAccessToken(String accessToken) {
        return super.checkAccessToken(accessToken);
    }

    // 根据ClientId 和 LoginId 获取openid
    @Override
    public String getOpenid(String clientId, Object loginId) {
        // 此为模拟数据，真实环境需要从数据库查询
        log.info("登录过程中，获取OPENID");
        return LoginHelper.getLoginUser(loginId).getOpenId();
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
            String tmp = StpUtil.getTokenValueByLoginId(loginId);
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