package com.jbm.util.sensitive;

import cn.hutool.core.util.StrUtil;

/**
 * 敏感信息脱敏工具
 */
public final class SensitiveDataUtils {

    private SensitiveDataUtils() {
    }

    public static String maskName(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return StrUtil.subPre(name, 1) + "*";
    }

    public static String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return StrUtil.subPre(mobile, 3) + "****" + StrUtil.subSuf(mobile, mobile.length() - 4);
    }

    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "*" + email.substring(at);
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
