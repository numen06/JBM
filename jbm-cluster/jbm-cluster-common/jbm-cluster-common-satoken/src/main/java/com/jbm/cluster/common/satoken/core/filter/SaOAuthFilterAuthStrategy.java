package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
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
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.common.basic.utils.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * Token过滤
 *
 * @Created wesley.zhang
 * @Date 2022/5/31 10:46
 * @Description TODO
 */
@Slf4j
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void run(Object r) {
        try {
            //如果是本机IP不要认证
            HttpServletRequest httpServletRequest = getCurrentRequest();
            if (httpServletRequest == null) return;

            String clientIp =IpUtils.getRequestIp(httpServletRequest); // Hutool 一行获取真实IP（自动处理代理）
            if (isLocalIp(clientIp)) {
                return; // 是本机请求，跳过认证
            }

            final String tokenValue = StpUtil.getTokenValue();
            if (StrUtil.isBlank(tokenValue)) {
                throw new SaOAuth2Exception("无效Token");
            }
            SaTokenInfo saTokenInfo = StpUtil.getTokenInfo();

            if (ObjectUtil.isNotEmpty(saTokenInfo)) {
                if (saTokenInfo.isLogin) {
                    if (saTokenInfo.tokenTimeout <= 0) {
                        throw new SaOAuth2Exception("Token已失效");
                    }
                    return;
                } else {
                    SaRequest req = SaHolder.getRequest();
                    if (StrUtil.isNotBlank(req.getHeader(SaIdUtil.ID_TOKEN))) {
                        SaIdUtil.checkCurrentRequestToken();
                        return;
                    }
                }
            }
            SaRequest req = SaHolder.getRequest();
            String clientId = null;
            AccessTokenModel accessTokenModel = SaOAuth2Util.getAccessToken(tokenValue);
            if (ObjectUtil.isNotEmpty(accessTokenModel)) {
                clientId = accessTokenModel.clientId;
                SaOAuth2Util.checkAccessToken(tokenValue);
//                // 先检查是否已过期
//                StpUtil.checkActivityTimeout();
//                // 检查通过后继续续签
//                StpUtil.updateLastActivityToNow();
            } else {
                ClientTokenModel clientTokenModel = SaOAuth2Util.getClientToken(tokenValue);
                if (ObjectUtil.isNotEmpty(clientTokenModel)) {
                    clientId = clientTokenModel.clientId;
                    log.info("Client Token Info:{}", JSON.toJSONString(clientTokenModel));
//                    SaOAuth2Util.checkClientToken(tokenValue);
                }
            }
            if (ObjectUtil.isEmpty(clientId)) {
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
