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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Token过滤
 *
 * @Created wesley.zhang
 * @Date 2022/5/31 10:46
 * @Description TODO
 */
public class SaOAuthFilterAuthStrategy implements SaFilterAuthStrategy {

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void run(Object r) {
        try {
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
//                    SaOAuth2Util.checkClientToken(tokenValue);
                }
            }
            if (ObjectUtil.isEmpty(clientId)) {
                throw new SaOAuth2Exception("无效的访问客户端");
            }
        } catch (Exception e) {
            throw e;
        }
    }

}
