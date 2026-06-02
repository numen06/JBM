package com.jbm.cluster.platform.gateway.logfilter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.api.entitys.basic.BaseApiKey;
import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.common.mysql.service.BaseApiKeyService;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.ApiSecurityConstants;
import com.jbm.cluster.core.constant.JbmTokenConstants;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TokenFilter implements AccessLogFilter {
    @Autowired
    private BaseAppService baseAppService;
    @Autowired
    private BaseApiKeyService baseApiKeyService;

    LoadingCache<String, BaseApp> appLoadingCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, BaseApp>() {
                @Override
                public @Nullable BaseApp load(@NonNull String appkey) throws Exception {
                    return baseAppService.getAppInfoByKey(appkey);
                }
            });

    LoadingCache<String, BaseApiKey> apiKeyLoadingCache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, BaseApiKey>() {
                @Override
                public @Nullable BaseApiKey load(@NonNull String apiKey) throws Exception {
                    return baseApiKeyService.getByApiKey(apiKey);
                }
            });

    @Override
    public void filter(GatewayLogInfo gatewayLogInfo, Map<String, String> headers) {
        if (gatewayLogInfo.getKeyId() != null) {
            return;
        }
        String appIdHeader = headers.get(ApiSecurityConstants.APP_ID);
        if (StrUtil.isNotBlank(appIdHeader)) {
            fillApiKeyLog(gatewayLogInfo, appIdHeader.trim());
            if (gatewayLogInfo.getKeyId() != null) {
                return;
            }
        }
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
                    return;
                }
                gatewayLogInfo.setAppKey(jbmLoginUser.getClientId());
                if (fillApiKeyLog(gatewayLogInfo, jbmLoginUser.getClientId())) {
                    return;
                }
                BaseApp app = appLoadingCache.get(jbmLoginUser.getClientId());
                gatewayLogInfo.setAppId(app.getAppId());
                gatewayLogInfo.setAppName(app.getAppName());
            } catch (Exception e) {
                log.debug("[TokenFilter]获取应用信息失败: {}", e.getMessage());
            }
        }
    }

    private boolean fillApiKeyLog(GatewayLogInfo gatewayLogInfo, String apiKey) {
        try {
            BaseApiKey row = apiKeyLoadingCache.get(apiKey);
            if (row == null) {
                return false;
            }
            gatewayLogInfo.setKeyId(row.getKeyId());
            gatewayLogInfo.setAppKey(row.getApiKey());
            gatewayLogInfo.setAppName(row.getKeyName());
            return true;
        } catch (Exception e) {
            log.debug("[TokenFilter]获取 API Key 信息失败: {}", e.getMessage());
            return false;
        }
    }
}
