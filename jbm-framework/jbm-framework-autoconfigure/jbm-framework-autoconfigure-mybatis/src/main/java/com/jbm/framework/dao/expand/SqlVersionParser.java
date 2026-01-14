package com.jbm.framework.dao.expand;

import cn.hutool.core.util.StrUtil;
import com.jbm.util.bean.Version;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL文件版本号解析和比较工具类
 * 
 * @author wesley
 */
@Slf4j
public class SqlVersionParser {

    // 版本号解析正则：支持日期格式（优先）和语义化版本（向后兼容）
    // 日期格式：20240101, 2024-01-01, 2024_01_01（统一规范化为8位数字）
    private final static Pattern DATE_PATTERN = Pattern.compile("^(\\d{4}[-_]?\\d{2}[-_]?\\d{2}|\\d{8})");
    // 语义化版本：V1.0.0, 1.0.0, V1, 1（向后兼容）
    private final static Pattern VERSION_PATTERN = Pattern.compile("^[Vv]?(\\d+(?:\\.\\d+)*(?:\\.\\d+)?)");

    /**
     * 从文件路径和文件名解析版本号
     * 优先级：1. 从文件夹路径解析 2. 从文件名解析 3. 返回null（无版本号）
     * 
     * @param filePath 文件路径（相对路径）
     * @param fileName 文件名
     * @return 版本号，如果无法解析则返回null
     */
    public static String parseVersionFromPath(String filePath, String fileName) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        
        // 1. 优先从文件夹路径解析版本号
        // 例如：sql/schema/20240101/webhook_index.sql -> 提取 20240101
        String[] pathParts = filePath.split("/");
        for (String part : pathParts) {
            if (StrUtil.isNotBlank(part)) {
                String version = extractVersion(part);
                if (version != null) {
                    log.debug("从文件夹路径解析版本号: {} -> {}", part, version);
                    return version;
                }
            }
        }
        
        // 2. 从文件名解析版本号
        // 例如：20240101__webhook_index.sql -> 提取 20240101
        if (StrUtil.isNotBlank(fileName)) {
            String version = extractVersion(fileName);
            if (version != null) {
                log.debug("从文件名解析版本号: {} -> {}", fileName, version);
                return version;
            }
        }
        
        // 3. 无法解析，返回null（向后兼容）
        return null;
    }

    /**
     * 从字符串中提取版本号
     * 优先级：1. 日期格式（8位数字：YYYYMMDD） 2. 语义化版本（向后兼容）
     * 支持格式：
     * - 日期：20240101, 2024-01-01, 2024_01_01（统一规范化为8位数字：20240101）
     * - 语义化版本：V1.0.0, 1.0.0, V1, 1
     * 
     * @param str 待解析的字符串
     * @return 版本号，如果无法解析则返回null
     */
    public static String extractVersion(String str) {
        if (StrUtil.isBlank(str)) {
            return null;
        }
        
        // 1. 优先尝试匹配日期格式（8位数字：YYYYMMDD）
        Matcher dateMatcher = DATE_PATTERN.matcher(str);
        if (dateMatcher.find()) {
            String date = dateMatcher.group(1);
            // 规范化日期：移除分隔符，统一为8位数字
            date = date.replaceAll("[-_]", "");
            if (date.length() == 8) {
                return date;
            }
        }
        
        // 2. 尝试匹配语义化版本（向后兼容）
        Matcher versionMatcher = VERSION_PATTERN.matcher(str);
        if (versionMatcher.find()) {
            return versionMatcher.group(1);
        }
        
        return null;
    }

    /**
     * 比较两个版本号
     * 优先级：1. 日期格式（8位数字：YYYYMMDD） 2. 语义化版本 3. 字符串比较
     * 
     * @param v1 版本号1
     * @param v2 版本号2
     * @return 负数表示v1 < v2, 0表示相等, 正数表示v1 > v2
     */
    public static int compareVersion(String v1, String v2) {
        if (StrUtil.isBlank(v1) && StrUtil.isBlank(v2)) {
            return 0;
        }
        if (StrUtil.isBlank(v1)) {
            return -1;
        }
        if (StrUtil.isBlank(v2)) {
            return 1;
        }
        
        // 1. 如果都是8位数字（日期格式：YYYYMMDD），直接比较
        if (v1.matches("^\\d{8}$") && v2.matches("^\\d{8}$")) {
            return v1.compareTo(v2);
        }
        
        // 2. 如果一个是日期格式，另一个不是，日期格式优先（更大）
        if (v1.matches("^\\d{8}$") && !v2.matches("^\\d{8}$")) {
            return 1; // 日期格式优先
        }
        if (!v1.matches("^\\d{8}$") && v2.matches("^\\d{8}$")) {
            return -1; // 日期格式优先
        }
        
        // 3. 尝试使用语义化版本比较（向后兼容）
        try {
            Version version1 = Version.parse(v1);
            Version version2 = Version.parse(v2);
            return version1.compareTo(version2);
        } catch (Exception e) {
            // 4. 如果无法解析，使用字符串比较（向后兼容）
            log.debug("版本号无法解析为Version对象，使用字符串比较: {} vs {}", v1, v2);
            return v1.compareTo(v2);
        }
    }
}
