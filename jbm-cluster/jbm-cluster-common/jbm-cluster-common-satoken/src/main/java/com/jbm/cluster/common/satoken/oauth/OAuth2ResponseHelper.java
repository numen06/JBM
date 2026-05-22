package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * OAuth2 响应与 Sa-Token token 对齐。
 */
public final class OAuth2ResponseHelper {

    private OAuth2ResponseHelper() {
    }

    public static void unifyAccessToken(AccessTokenModel token) {
        if (token == null) {
            return;
        }
        String currentToken = resolveSaTokenValue();
        if (StrUtil.isNotBlank(currentToken)) {
            token.accessToken = currentToken;
        }
    }

    @SuppressWarnings("unchecked")
    public static void unifyAccessTokenInResult(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        Object accessToken = data.get("access_token");
        if (accessToken == null) {
            return;
        }
        String currentToken = resolveSaTokenValue();
        if (StrUtil.isNotBlank(currentToken)) {
            data.put("access_token", currentToken);
            data.put("token_type", SaManager.getConfig().getTokenPrefix());
        }
    }

    private static String resolveSaTokenValue() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getTokenValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}