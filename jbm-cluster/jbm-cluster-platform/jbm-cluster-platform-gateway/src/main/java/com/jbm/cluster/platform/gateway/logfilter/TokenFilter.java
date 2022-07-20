package com.jbm.cluster.platform.gateway.logfilter;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.api.service.fegin.client.BaseAppServiceClient;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmTokenConstants;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TokenFilter implements AccessLogFilter {
    @Autowired
    private BaseAppServiceClient baseAppServiceClient;

    @Override
    public void filter(GatewayLogInfo gatewayLogInfo, Map<String, String> headers) {
        String authorization = headers.get(JbmTokenConstants.AUTHENTICATION);
        if (StrUtil.isEmpty(authorization)) {
            return;
        }
        String token = StrUtil.trim(StrUtil.removePrefix(authorization, JbmTokenConstants.PREFIX));
        JbmLoginUser jbmLoginUser = LoginHelper.getLoginUser(token);
        if (ObjectUtil.isNotEmpty(jbmLoginUser)) {
            gatewayLogInfo.setRequestUserId(jbmLoginUser.getUserId());
            gatewayLogInfo.setRequestRealName(jbmLoginUser.getRealName());
            try {
                if (StrUtil.isBlank(jbmLoginUser.getClientId())) {
                    jbmLoginUser.setClientId(SaOAuth2Util.getAccessToken(jbmLoginUser.getToken()).clientId);
                }
                gatewayLogInfo.setAppKey(jbmLoginUser.getClientId());
                BaseApp app = appLoadingCache.get(jbmLoginUser.getClientId());
                gatewayLogInfo.setAppId(app.getAppId());
                gatewayLogInfo.setAppName(app.getAppName());
            } catch (Exception e) {

            }
            //gatewayLogs.setAuthentication(jbmLoginUser.getToken());
        }
    }

    LoadingCache<String, BaseApp> appLoadingCache = Caffeine.newBuilder()
            //一小时没有读取释放
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, BaseApp>() {
                @Override
                public @Nullable BaseApp load(@NonNull String appkey) throws Exception {
                    return baseAppServiceClient.getAppByKey(appkey).getResult();
                }
            });

}
