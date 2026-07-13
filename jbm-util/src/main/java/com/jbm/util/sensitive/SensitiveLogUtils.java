package com.jbm.util.sensitive;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Masks credentials that may be captured in request and response logs.
 */
public final class SensitiveLogUtils {

    public static final String MASKED_VALUE = "[REDACTED]";

    private static final String SENSITIVE_KEY_PATTERN =
            "(?:authorization|proxy[-_]?authorization|cookie|set[-_]?cookie|password|passwd|pwd|"
                    + "client[-_]?secret|secret[-_]?key|access[-_]?token|refresh[-_]?token|client[-_]?token|"
                    + "id[-_]?token|sa[-_]?token|[a-z0-9_-]*token)";

    private static final Pattern DOUBLE_QUOTED_VALUE_PATTERN = Pattern.compile(
            "(?i)(\\\"" + SENSITIVE_KEY_PATTERN + "\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final Pattern SINGLE_QUOTED_VALUE_PATTERN = Pattern.compile(
            "(?i)('" + SENSITIVE_KEY_PATTERN + "'\\s*:\\s*')[^']*(')");
    private static final Pattern FORM_VALUE_PATTERN = Pattern.compile(
            "(?i)((?:^|[?&;\\s])" + SENSITIVE_KEY_PATTERN + "=)[^&;\\s]*");
    private static final Pattern HEADER_VALUE_PATTERN = Pattern.compile(
            "(?im)^(" + SENSITIVE_KEY_PATTERN + "\\s*:\\s*).*$");
    private static final Pattern AUTH_SCHEME_PATTERN = Pattern.compile(
            "(?i)\\b(Bearer|Basic|Token)\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern JWT_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+(?![A-Za-z0-9_-])");

    private SensitiveLogUtils() {
    }

    public static String maskTokens(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        String maskedJson = maskJson(content);
        return maskPlainText(maskedJson == null ? content : maskedJson);
    }

    private static String maskJson(String content) {
        String trimmed = content.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return null;
        }
        try {
            Object value = JSON.parse(content);
            if (!(value instanceof JSONObject) && !(value instanceof JSONArray)) {
                return null;
            }
            maskJsonValue(value);
            return JSON.toJSONString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void maskJsonValue(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                if (isSensitiveKey(entry.getKey())) {
                    entry.setValue(MASKED_VALUE);
                } else {
                    entry.setValue(maskValue(entry.getValue()));
                }
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.size(); i++) {
                array.set(i, maskValue(array.get(i)));
            }
        }
    }

    private static Object maskValue(Object value) {
        if (value instanceof JSONObject || value instanceof JSONArray) {
            maskJsonValue(value);
            return value;
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            String nestedJson = maskJson(stringValue);
            return maskPlainText(nestedJson == null ? stringValue : nestedJson);
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.endsWith("token")
                || normalized.equals("authorization")
                || normalized.equals("proxyauthorization")
                || normalized.startsWith("cookie")
                || normalized.equals("setcookie")
                || normalized.contains("password")
                || normalized.equals("passwd")
                || normalized.equals("pwd")
                || normalized.endsWith("secret");
    }

    private static String maskPlainText(String content) {
        String masked = DOUBLE_QUOTED_VALUE_PATTERN.matcher(content)
                .replaceAll("$1" + MASKED_VALUE + "$2");
        masked = SINGLE_QUOTED_VALUE_PATTERN.matcher(masked)
                .replaceAll("$1" + MASKED_VALUE + "$2");
        masked = FORM_VALUE_PATTERN.matcher(masked)
                .replaceAll("$1" + MASKED_VALUE);
        masked = HEADER_VALUE_PATTERN.matcher(masked)
                .replaceAll("$1" + MASKED_VALUE);
        masked = AUTH_SCHEME_PATTERN.matcher(masked)
                .replaceAll("$1 " + MASKED_VALUE);
        return JWT_PATTERN.matcher(masked).replaceAll(MASKED_VALUE);
    }
}
