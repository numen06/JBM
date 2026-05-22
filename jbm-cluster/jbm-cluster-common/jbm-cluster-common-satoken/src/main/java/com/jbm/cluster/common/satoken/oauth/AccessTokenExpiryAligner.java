package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AccessTokenExpiryAligner {

    private AccessTokenExpiryAligner() {
    }

    public static long resolveSaTokenRemainingSeconds(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            return -2;
        }
        String tokenName = SaManager.getConfig().getTokenName();
        if (StrUtil.isBlank(tokenName)) {
            tokenName = "Authorization";
        }
        SaTokenDao dao = SaManager.getSaTokenDao();
        long tokenTtl = dao.getTimeout(tokenName + ":login:token:" + accessToken);
        long activityTtl = dao.getTimeout(tokenName + ":login:last-activity:" + accessToken);

        long effective = Long.MAX_VALUE;
        if (tokenTtl > 0) {
            effective = Math.min(effective, tokenTtl);
        }
        if (activityTtl > 0) {
            effective = Math.min(effective, activityTtl);
        }
        if (effective != Long.MAX_VALUE) {
            return effective;
        }

        try {
            Long activityByApi = StpUtil.stpLogic.getTokenActivityTimeoutByToken(accessToken);
            if (activityByApi != null && activityByApi > 0) {
                return activityByApi;
            }
        } catch (Exception ignored) {
        }

        long configured = SaManager.getConfig().getTimeout();
        return configured > 0 ? configured : -2;
    }

    public static void alignAccessTokenModel(AccessTokenModel accessToken) {
        if (accessToken == null || StrUtil.isBlank(accessToken.accessToken)) {
            return;
        }
        long remaining = resolveSaTokenRemainingSeconds(accessToken.accessToken);
        if (remaining <= 0) {
            return;
        }
        accessToken.expiresTime = System.currentTimeMillis() + remaining * 1000L;
    }

    public static void tryResyncOAuth2AccessToken(String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            return;
        }
        try {
            AccessTokenModel model = SaOAuth2Util.getAccessToken(accessToken);
            if (model == null) {
                return;
            }
            alignAccessTokenModel(model);
            SaOAuth2Template template = SpringUtil.getBean(SaOAuth2Template.class);
            template.saveAccessToken(model);
            template.saveAccessTokenIndex(model);
        } catch (Exception ignored) {
        }
    }
}