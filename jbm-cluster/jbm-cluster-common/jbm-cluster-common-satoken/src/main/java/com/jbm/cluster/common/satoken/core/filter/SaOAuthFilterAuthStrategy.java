package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.common.basic.context.SecurityContextHolder;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * Token过滤
 *
 * @Created wesley.zhang
 * @Date 2022/5/31 10:46
 * @Description TODO
 */
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(SaOAuthFilterAuthStrategy.class);

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void run(Object r) {
        try {
            HttpServletRequest httpServletRequest = getCurrentRequest();
            if (httpServletRequest == null) return;

            String clientIp =IpUtils.getRequestIp(httpServletRequest);
            if (isLocalIp(clientIp)) {
                return;
            }

            // [互信诊断] 打印接收到的关键 header
            log.debug("[互信诊断] SaOAuthFilter 收到请求: clientIp={}, requestURI={}, Authorization={}, internal={}",
                    clientIp, httpServletRequest.getRequestURI(),
                    httpServletRequest.getHeader("Authorization") != null
                            ? httpServletRequest.getHeader("Authorization").substring(0, Math.min(50, httpServletRequest.getHeader("Authorization").length())) + "..."
                            : "null",
                    httpServletRequest.getHeader(JbmSecurityConstants.INTERNAL_SERVICE));

            final String tokenValue = StpUtil.getTokenValue();
            log.debug("[互信诊断] StpUtil.getTokenValue()={}", tokenValue != null ? tokenValue.substring(0, Math.min(30, tokenValue.length())) + "..." : "null");
            if (StrUtil.isBlank(tokenValue)) {
                throw new SaOAuth2Exception("无效Token");
            }
            SaTokenInfo saTokenInfo = StpUtil.getTokenInfo();
            log.debug("[互信诊断] SaTokenInfo: isLogin={}, tokenTimeout={}", saTokenInfo != null ? saTokenInfo.isLogin : "null", saTokenInfo != null ? saTokenInfo.tokenTimeout : "null");

            if (ObjectUtil.isNotEmpty(saTokenInfo)) {
                if (saTokenInfo.isLogin) {
                    if (saTokenInfo.tokenTimeout <= 0) {
                        throw new SaOAuth2Exception("Token已失效");
                    }
                    log.debug("[互信诊断] 认证通过路径: StpUtil用户Token有效");
                    return;
                }
                log.debug("[互信诊断] StpUtil未登录，继续走OAuth2 ClientToken/AccessToken校验");
            }
            SaRequest req = SaHolder.getRequest();
            String clientId = null;
            AccessTokenModel accessTokenModel = SaOAuth2Util.getAccessToken(tokenValue);
            if (ObjectUtil.isNotEmpty(accessTokenModel)) {
                clientId = accessTokenModel.clientId;
                SaOAuth2Util.checkAccessToken(tokenValue);
                log.debug("[互信诊断] 认证通过路径: AccessToken有效, clientId={}", clientId);
            } else {
                ClientTokenModel clientTokenModel = SaOAuth2Util.getClientToken(tokenValue);
                log.debug("[互信诊断] AccessToken未找到, 查找ClientToken结果: {}", clientTokenModel != null ? "找到, clientId=" + clientTokenModel.clientId : "未找到");
                if (ObjectUtil.isNotEmpty(clientTokenModel)) {
                    clientId = clientTokenModel.clientId;
                    log.debug("[互信诊断] 认证通过路径: ClientToken(Redis)有效, clientId={}", clientId);
                    recordInternalCaller(httpServletRequest);
                    return;
                }
            }
            if (ObjectUtil.isEmpty(clientId)) {
                log.debug("[互信诊断] 认证失败: 所有分支均未匹配, tokenValue={}", tokenValue != null ? tokenValue.substring(0, Math.min(30, tokenValue.length())) + "..." : "null");
                throw new SaOAuth2Exception(StrUtil.format("无效的访问客户端:{}", clientId));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 获取当前 HttpServletRequest（Spring 环境）
     */
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

    /**
     * 判断是否为本地回环地址（支持 127.x.x.x 和 ::1）
     */
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
