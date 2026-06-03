package com.jbm.cluster.common.mysql.service.openapi;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

public final class OpenApiHubSupport {

    private OpenApiHubSupport() {
    }

    public static String operationKey(String serviceId, String method, String path) {
        return serviceId + ":" + method.toUpperCase() + ":" + normalizePath(path);
    }

    public static String normalizePath(String path) {
        if (StrUtil.isBlank(path)) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    public static String sha256(String content) {
        return "sha256:" + DigestUtil.sha256Hex(StrUtil.nullToEmpty(content));
    }
}
