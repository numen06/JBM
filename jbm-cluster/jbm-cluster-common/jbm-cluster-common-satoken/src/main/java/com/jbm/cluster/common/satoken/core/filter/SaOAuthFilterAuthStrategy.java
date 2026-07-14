package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * Token 过滤：区分用户 Token 与服务 ClientToken 两条认证轨道。
 */
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {

    private static final Logger log = LoggerFactory.getLogger(SaOAuthFilterAuthStrategy.class);

    @Autowired
    private JbmClusterProperties jbmClusterProperties;

    @Override
    public void run(Object r) {
        HttpServletRequest httpServletRequest = getCurrentRequest();
        if (httpServletRequest == null) {
            return;
        }

        String clientIp = IpUtils.getRequestIp(httpServletRequest);
        if (Boolean.TRUE.equals(jbmClusterProperties.getAllowLocalBypass()) && isLocalIp(clientIp)) {
            log.debug("[互信诊断] 本地回环地址跳过认证: clientIp={}", clientIp);
            return;
        }

        log.debug("[互信诊断] SaOAuthFilter 收到请求: clientIp={}, requestURI={}, Authorization={}, Satoken-Id-Token={}",
                clientIp, httpServletRequest.getRequestURI(),
                maskHeader(httpServletRequest.getHeader("Authorization")),
                maskHeader(httpServletRequest.getHeader(SaIdUtil.ID_TOKEN)));

        String idToken = httpServletRequest.getHeader(SaIdUtil.ID_TOKEN);
        String requestSource = httpServletRequest.getHeader(JbmSecurityConstants.FROM_SOURCE);
        if (JbmSecurityConstants.INNER.equals(requestSource) && StrUtil.isNotBlank(idToken)) {
            SaIdUtil.checkCurrentRequestToken();
            log.debug("[互信诊断] 认证通过路径: 内部Id-Token验证通过");
            return;
        }

        final String tokenValue = StpUtil.getTokenValue();
        if (StrUtil.isBlank(tokenValue)) {
            throw new SaOAuth2Exception("无效Token");
        }

        SaTokenInfo saTokenInfo = StpUtil.getTokenInfo();
        if (ObjectUtil.isNotEmpty(saTokenInfo) && saTokenInfo.isLogin) {
            if (saTokenInfo.tokenTimeout <= 0) {
                throw new SaOAuth2Exception("Token已失效");
            }
            log.debug("[互信诊断] 认证通过路径: StpUtil用户Token有效");
            return;
        }

        AccessTokenModel accessTokenModel = SaOAuth2Util.getAccessToken(tokenValue);
        if (ObjectUtil.isNotEmpty(accessTokenModel)) {
            SaOAuth2Util.checkAccessToken(tokenValue);
            log.debug("[互信诊断] 认证通过路径: AccessToken有效, clientId={}", accessTokenModel.clientId);
            return;
        }

        ClientTokenModel clientTokenModel = SaOAuth2Util.getClientToken(tokenValue);
        if (ObjectUtil.isNotEmpty(clientTokenModel)) {
            SaOAuth2Util.checkClientToken(tokenValue);
            log.debug("[互信诊断] 认证通过路径: ClientToken有效, clientId={}", clientTokenModel.clientId);
            return;
        }

        log.debug("[互信诊断] 认证失败: tokenValue={}", maskToken(tokenValue));
        throw new SaOAuth2Exception("无效的访问客户端");
    }

    private static String maskHeader(String header) {
        if (header == null) {
            return "null";
        }
        return header.substring(0, Math.min(50, header.length())) + "...";
    }

    private static String maskToken(String tokenValue) {
        if (tokenValue == null) {
            return "null";
        }
        return tokenValue.substring(0, Math.min(30, tokenValue.length())) + "...";
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            return (HttpServletRequest)
                    org.springframework.web.context.request.RequestContextHolder
                            .currentRequestAttributes()
                            .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isLocalIp(String ip) {
        if (ObjectUtil.isEmpty(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
