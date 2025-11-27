package com.jbm.cluster.common.basic.module.log;

import cn.hutool.core.util.StrUtil;

/**
 * 处理业务日志 trace/file 相关的通用工具。
 */
public final class BusinessLogTraceUtils {

    private BusinessLogTraceUtils() {
    }

    public static String traceLockKey(String logId) {
        return sanitizeFileName(StrUtil.blankToDefault(logId, "unknown"));
    }

    public static String sanitizeFileName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "unknown";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

