package com.jbm.cluster.api.model.gateway;

import com.jbm.util.sensitive.SensitiveLogUtils;

/**
 * Applies log token masking consistently at collection, storage and query boundaries.
 */
public final class GatewayLogInfoSanitizer {

    private GatewayLogInfoSanitizer() {
    }

    public static <T extends GatewayLogInfo> T sanitize(T logInfo) {
        if (logInfo == null) {
            return null;
        }
        logInfo.setPath(SensitiveLogUtils.maskTokens(logInfo.getPath()));
        logInfo.setApiPath(SensitiveLogUtils.maskTokens(logInfo.getApiPath()));
        logInfo.setParams(SensitiveLogUtils.maskTokens(logInfo.getParams()));
        logInfo.setHeaders(SensitiveLogUtils.maskTokens(logInfo.getHeaders()));
        logInfo.setResponseBody(SensitiveLogUtils.maskTokens(logInfo.getResponseBody()));
        logInfo.setAuthentication(SensitiveLogUtils.maskTokens(logInfo.getAuthentication()));
        logInfo.setError(SensitiveLogUtils.maskTokens(logInfo.getError()));
        return logInfo;
    }
}
