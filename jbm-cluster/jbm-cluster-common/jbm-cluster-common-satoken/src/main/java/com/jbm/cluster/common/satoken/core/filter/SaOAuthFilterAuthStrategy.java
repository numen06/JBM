package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.basic.context.SecurityContextHolder;
import com.jbm.cluster.common.satoken.config.JbmAuthProperties;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtContext;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtPrincipal;
import com.jbm.cluster.common.satoken.standardjwt.StandardJwtSupport;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.cluster.core.constant.JbmTokenConstants;
import cn.hutool.extra.spring.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * 下游服务 Token 校验：标准 OAuth2 JWT 优先，兼容历史 Sa-Token / OAuth2 AccessToken。
 * <p>经 Gateway 转发时同时带用户 Authorization 与内部服务 JWT，须先绑定用户态，避免仅内部服务 JWT 通过但 @SaCheckLogin 仍 401。</p>
 */
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {

    private static final Logger log = LoggerFactory.getLogger(SaOAuthFilterAuthStrategy.class);

    @Override
    public void run(Object r) {
        HttpServletRequest httpServletRequest = getCurrentRequest();
        if (httpServletRequest == null) {
            return;
        }
        JbmAuthProperties authProperties = currentAuthProperties();

        String userBearer = extractBearerToken(
                httpServletRequest.getHeader(JbmSecurityConstants.AUTHORIZATION_HEADER));

        log.debug("[认证] requestURI={}, Authorization={}, internalAuthorization={}, internal={}",
                httpServletRequest.getRequestURI(),
                mask(httpServletRequest.getHeader(JbmSecurityConstants.AUTHORIZATION_HEADER)),
                mask(httpServletRequest.getHeader(JbmSecurityConstants.INTERNAL_AUTHORIZATION_HEADER)),
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
            log.debug("[认证] 用户 Token 未登录，尝试标准 OAuth JWT / Sa-Token 兼容");
        }

        if (authProperties.isOauthEnabled() && tryBindStandardJwtToken(userBearer)) {
            log.debug("[认证] 通过: 标准 OAuth JWT 已解析");
            return;
        }

        if (authProperties.isRedisEnabled() && tryBindRedisToken(userBearer)) {
            log.debug("[认证] 通过: Sa-Token Redis 会话已解析");
            return;
        }

        if (authProperties.isRedisEnabled() && tryBindOAuthAccessToken(httpServletRequest, userBearer)) {
            log.debug("[认证] 通过: 历史 OAuth2 AccessToken 已绑定用户态");
            return;
        }

        if (StrUtil.isNotBlank(userBearer)) {
            log.debug("[认证] 用户 Bearer 无效");
            throw NotLoginException.newInstance(StpUtil.getLoginType(), NotLoginException.INVALID_TOKEN);
        }

        if (isGatewayApiKeyCaller(httpServletRequest)) {
            recordInternalCaller(httpServletRequest);
            log.debug("[认证] 通过: Gateway API Key 已授权");
            return;
        }

        if (authProperties.isOauthEnabled() && tryBindInternalServiceJwt(httpServletRequest)) {
            recordInternalCaller(httpServletRequest);
            log.debug("[认证] 通过: OAuth2 Client Credentials 内部互信");
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
     * Redis 模式下 token 映射在 Redis 但 session 可能不存在，尝试仅凭 token 解析 loginId。
     */
    private static boolean tryBindRedisToken(String token) {
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
            log.debug("[认证] Sa-Token Redis 会话解析失败: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryBindStandardJwtToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        StandardJwtPrincipal principal = StandardJwtSupport.bind(token);
        if (principal == null) {
            return false;
        }
        LoginHelper.setLoginUserCache(principal.getLoginUser());
        StpUtil.setTokenValue(token);
        return true;
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

    private static boolean tryBindInternalServiceJwt(HttpServletRequest request) {
        if (request == null || StrUtil.isBlank(request.getHeader(JbmSecurityConstants.INTERNAL_SERVICE))) {
            return false;
        }
        String token = extractBearerToken(request.getHeader(JbmSecurityConstants.INTERNAL_AUTHORIZATION_HEADER));
        StandardJwtPrincipal principal = StandardJwtSupport.bind(token);
        if (principal == null) {
            return false;
        }
        LoginHelper.setLoginUserCache(principal.getLoginUser());
        StpUtil.setTokenValue(token);
        return true;
    }

    private static JbmAuthProperties currentAuthProperties() {
        try {
            JbmAuthProperties properties = SpringUtil.getBean(JbmAuthProperties.class);
            if (properties != null) {
                return properties;
            }
        } catch (Exception ignored) {
        }
        return new JbmAuthProperties();
    }
}
