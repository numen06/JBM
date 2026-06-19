package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;

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
        if (isJwt(token.accessToken)) {
            AccessTokenExpiryAligner.alignAccessTokenModel(token);
            return;
        }
        String currentToken = resolveSaTokenValue();
        if (StrUtil.isNotBlank(currentToken)) {
            token.accessToken = currentToken;
        }
        AccessTokenExpiryAligner.alignAccessTokenModel(token);
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
        if (isJwt(String.valueOf(accessToken))) {
            attachMustChangePasswordFlag(data);
            return;
        }
        String currentToken = resolveSaTokenValue();
        if (StrUtil.isNotBlank(currentToken)) {
            data.put("access_token", currentToken);
            data.put("token_type", SaManager.getConfig().getTokenPrefix());
        }
        attachMustChangePasswordFlag(data);
    }

    /**
     * OAuth2 token 响应中附带首次登录须改密标记。
     */
    public static void attachMustChangePasswordFlag(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
            JbmLoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null && Boolean.TRUE.equals(loginUser.getMustChangePassword())) {
                data.put("must_change_password", true);
            }
        } catch (Exception ignored) {
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

    private static boolean isJwt(String token) {
        return StrUtil.isNotBlank(token) && token.split("\\.").length == 3;
    }
}
