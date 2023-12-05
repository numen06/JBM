package com.jbm.cluster.common.satoken.oauth;


import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * jbm默认自己节点的oauth认证
 *
 * @Created wesley.zhang
 * @Date 2022/5/15 13:08
 * @Description TODO
 */
@Slf4j
public class JbmNodeOAuth2TemplateImpl extends SaOAuth2Template {

    private LoadingCache<String, ClientTokenModel> clientTokenModelLoadingCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
//            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(key -> super.generateClientToken(key + "-" + DateUtil.format(DateTime.now(), DatePattern.PURE_DATETIME_PATTERN), "*")
            );

    @Override
    public ClientTokenModel generateClientToken(String clientId, String scope) {
        try {
            return clientTokenModelLoadingCache.get(clientId);
        } catch (Exception e) {
            log.error("取应用程序TOKEN失败", e);
            throw e;
        }
    }

    @Override
    public SaClientModel getClientModel(String clientId) {
        if (StrUtil.contains(clientId, SpringUtil.getApplicationName())) {
            return new SaClientModel()
                    .setClientId(clientId)
                    .setClientSecret(UUID.randomUUID().toString())
                    .setAllowUrl("*")
                    .setContractScope("*")
                    .setIsAutoMode(true);
        }
        ClientTokenModel clientTokenModel = getClientToken(getClientTokenValue(clientId));
        if (ObjectUtil.isNotEmpty(clientTokenModel)) {
            return new SaClientModel()
                    .setClientId(clientTokenModel.clientId)
                    .setClientSecret(UUID.randomUUID().toString())
                    .setAllowUrl("*")
                    .setContractScope("*")
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
        log.info("登录过程中，获取OPENID");
        if (ObjectUtil.isEmpty(LoginHelper.getLoginUser(loginId))) {
            return StrUtil.toString(loginId);
        }
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
        String token = null;
        try {
            String tmp = StpUtil.getTokenValueByLoginId(loginId);
            if (StrUtil.isNotEmpty(tmp)) {
                token = tmp;
            } else {
                //没有的话，创建一个
                token = StpUtil.createLoginSession(loginId);
            }
        } catch (Exception e) {
            log.error("获取用户token失败", e);
        }
        return token;
    }
    // -------------- 其它需要重写的函数


    @Override
    public AccessTokenModel refreshAccessToken(String refreshToken) {
        AccessTokenModel accessTokenModel = super.refreshAccessToken(refreshToken);
        StpUtil.updateLastActivityToNow();
        log.info("刷新后Token过期时间:{}", StpUtil.getTokenInfo().getTokenActivityTimeout());
        return accessTokenModel;
    }

    @Override
    public String randomClientToken(String clientId, String scope) {
        //使用IdToken接管
        final String idToken = SaIdUtil.getToken();
//        SaIdUtil.saIdTemplate.saveToken(idToken);
        return idToken;
    }
}