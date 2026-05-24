package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.basic.context.SecurityContextHolder;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.core.constant.JbmTokenConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * 下游服务 Token 校验：用户 Sa-Token / OAuth2 AccessToken，或 Gateway/Feign Id-Token 内部互信。
 * <p>经 Gateway 转发时同时带用户 Authorization 与 Id-Token，须先绑定 OAuth 用户态，避免仅 Id-Token 通过但 @SaCheckLogin 仍 401。</p>
 */
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {

    private static final Logger log = LoggerFactory.getLogger(SaOAuthFilterAuthStrategy.class);

    @Override
    public void run(Object r) {
        HttpServletRequest httpServletRequest = getCurrentRequest();
        if (httpServletRequest == null) {
            return;
        }

        String userBearer = extractBearerToken(
                httpServletRequest.getHeader(JbmSecurityConstants.AUTHORIZATION_HEADER));

        log.debug("[认证] requestURI={}, Authorization={}, idToken={}, internal={}",
                httpServletRequest.getRequestURI(),
                mask(httpServletRequest.getHeader(JbmSecurityConstants.AUTHORIZATION_HEADER)),
                mask(httpServletRequest.getHeader(SaIdUtil.ID_TOKEN)),
                httpServletRequest.getHeader(JbmSecurityConstants.INTERNAL_SERVICE));

        if (StrUtil.isNotBlank(userBearer)) {
            StpUtil.setTokenValue(userBearer);
        }

        try {
            StpUtil.checkLogin();
            LoginHelper.initCache();
            log.debug("[认证] 通过: 用户 Token 有效");
            return;
        } catch (NotLoginException ignored) {
            log.debug("[认证] 用户 Token 未登录，尝试 OAuth2 / JWT / Id-Token");
        }

        if (tryBindOAuthAccessToken(httpServletRequest, userBearer)) {
            log.debug("[认证] 通过: OAuth2 AccessToken 已绑定用户态");
            return;
        }

        if (tryBindJwtToken(userBearer)) {
            log.debug("[认证] 通过: JWT Token 已解析");
            return;
        }

        if (StrUtil.isNotBlank(userBearer)) {
            log.debug("[认证] 用户 Bearer 无效，拒绝仅 Id-Token 放行");
            throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.INVALID_TOKEN);
        }

        if (isGatewayApiKeyCaller(httpServletRequest)) {
            recordInternalCaller(httpServletRequest);
            log.debug("[认证] 通过: Gateway API Key 已授权");
            return;
        }

        String idToken = httpServletRequest.getHeader(SaIdUtil.ID_TOKEN);
        if (StrUtil.isNotBlank(idToken) && SaIdUtil.isValid(idToken)) {
            recordInternalCaller(httpServletRequest);
            log.debug("[认证] 通过: Id-Token 内部互信");
            return;
        }
        throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.INVALID_TOKEN);
    }

    /**
     * 将 Authorization 中的 OAuth2 AccessToken 绑定到当前请求的 Sa-Token 上下文（供 @SaCheckLogin 使用）。
     */
    private static boolean tryBindOAuthAccessToken(HttpServletRequest request, String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            AccessTokenModel accessToken = SaOAuth2Util.getAccessToken(token);
            if (accessToken == null) {
                accessToken = SaOAuth2Util.checkAccessToken(token);
            }
            if (accessToken == null) {
                return false;
            }
            bindSaTokenSession(token, accessToken);
            return resolveLoginId(token, accessToken) != null;
        } catch (Exception accessEx) {
            log.debug("[认证] 非 OAuth2 AccessToken: {}", accessEx.getMessage());
        }
        try {
            ClientTokenModel clientToken = SaOAuth2Util.checkClientToken(token);
            if (clientToken != null) {
                StpUtil.setTokenValue(token);
                return true;
            }
        } catch (Exception ignored) {
            // not a client token
        }
        return false;
    }

    /**
     * JWT 模式下 token 映射在 Redis 但 session 可能不存在，尝试仅凭 token 解析 loginId。
     */
    private static boolean tryBindJwtToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            StpUtil.setTokenValue(token);
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return false;
            }
            LoginHelper.initCache();
            hydrateLoginUserCache(loginId);
            return true;
        } catch (Exception e) {
            log.debug("[认证] JWT 解析失败: {}", e.getMessage());
            return false;
        }
    }

    private static Object resolveLoginId(String token, AccessTokenModel accessToken) {
        try {
            return StpUtil.getLoginId();
        } catch (Exception ignored) {
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                return loginId;
            }
        } catch (Exception ignored) {
        }
        if (accessToken != null && accessToken.loginId != null) {
            return accessToken.loginId;
        }
        try {
            return SaOAuth2Util.getLoginIdByAccessToken(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void bindSaTokenSession(String token, AccessTokenModel accessToken) {
        StpUtil.setTokenValue(token);
        LoginHelper.initCache();
        if (LoginHelper.softGetLoginUser() != null) {
            return;
        }
        JbmLoginUser loginUser = null;
        try {
            loginUser = LoginHelper.getLoginUser(token);
        } catch (Exception ignored) {
        }
        if (loginUser != null) {
            cacheLoginUser(loginUser);
            return;
        }
        Object loginId = accessToken.loginId;
        if (loginId == null) {
            try {
                loginId = SaOAuth2Util.getLoginIdByAccessToken(token);
            } catch (Exception ignored) {
            }
        }
        if (loginId == null) {
            try {
                loginId = StpUtil.getLoginIdByToken(token);
            } catch (Exception ignored) {
            }
        }
        if (loginId != null) {
            hydrateLoginUserCache(loginId);
        }
    }

    private static void hydrateLoginUserCache(Object loginId) {
        if (loginId == null || LoginHelper.softGetLoginUser() != null) {
            return;
        }
        try {
            JbmLoginUser loginUser = LoginHelper.getLoginUser(loginId);
            cacheLoginUser(loginUser);
        } catch (Exception ignored) {
        }
    }

    private static void cacheLoginUser(JbmLoginUser loginUser) {
        if (loginUser == null) {
            return;
        }
        try {
            LoginHelper.setLoginUser(loginUser);
        } catch (Exception ignored) {
            LoginHelper.setLoginUserCache(loginUser);
        }
    }

    private static String extractBearerToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return null;
        }
        if (authorization.startsWith(JbmTokenConstants.PREFIX)) {
            return authorization.substring(JbmTokenConstants.PREFIX.length()).trim();
        }
        return authorization.trim();
    }

    private static String mask(String value) {
        if (StrUtil.isBlank(value)) {
            return "null";
        }
        return value.substring(0, Math.min(30, value.length())) + "...";
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            Object source = cn.dev33.satoken.context.SaHolder.getRequest().getSource();
            if (source instanceof HttpServletRequest) {
                return (HttpServletRequest) source;
            }
        } catch (Exception ignored) {
        }
        try {
            return (HttpServletRequest)
                    org.springframework.web.context.request.RequestContextHolder
                            .currentRequestAttributes()
                            .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST);
        } catch (Exception e) {
            return null;
        }
    }

    private static void recordInternalCaller(HttpServletRequest request) {
        String fromService = request.getHeader(JbmSecurityConstants.INTERNAL_SERVICE);
        if (StrUtil.isNotBlank(fromService)) {
            SecurityContextHolder.set(JbmSecurityConstants.FROM_SERVICE, fromService);
            SecurityContextHolder.set(JbmSecurityConstants.FROM_INSTANCE,
                    request.getHeader(JbmSecurityConstants.INTERNAL_INSTANCE));
            log.debug("[互信] 内部调用 from={}:{}", fromService,
                    request.getHeader(JbmSecurityConstants.INTERNAL_INSTANCE));
        }
    }
    private static boolean isGatewayApiKeyCaller(HttpServletRequest request) {
        return request != null
                && StrUtil.isNotBlank(request.getHeader(JbmSecurityConstants.GATEWAY_API_KEY_ID))
                && StrUtil.isNotBlank(request.getHeader(JbmSecurityConstants.INTERNAL_SERVICE));
    }
}
