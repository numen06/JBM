package com.jbm.util;

import cn.hutool.core.util.ReUtil;

/**
 * Date Created  2014-2-19
 *
 * @author wesley
 * @version 1.0
 */
public abstract class PatternMatchUtils {

    /**
     * Match a String against the given pattern, supporting the following simple
     * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
     * arbitrary number of pattern parts), as well as direct equality.
     *
     * @param pattern the pattern to match against
     * @param str     the String to match
     * @return whether the String matches the given pattern
     */
    public static boolean simpleMatch(String pattern, String str) {
        if (pattern == null || str == null) {
            return false;
        }
        int firstIndex = pattern.indexOf('*');
        if (firstIndex == -1) {
            return pattern.equals(str);
        }
        if (firstIndex == 0) {
            if (pattern.length() == 1) {
                return true;
            }
            int nextIndex = pattern.indexOf('*', firstIndex + 1);
            if (nextIndex == -1) {
                return str.endsWith(pattern.substring(1));
            }
            String part = pattern.substring(1, nextIndex);
            int partIndex = str.indexOf(part);
            while (partIndex != -1) {
                if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
                    return true;
                }
                partIndex = str.indexOf(part, partIndex + 1);
            }
            return false;
        }
        return (str.length() >= firstIndex &&
                pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) &&
                simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
    }

    /**
     * Match a String against the given patterns, supporting the following simple
     * pattern styles: "xxx*", "*xxx", "*xxx*" and "xxx*yyy" matches (with an
     * arbitrary number of pattern parts), as well as direct equality.
     *
     * @param patterns the patterns to match against
     * @param str      the String to match
     * @return whether the String matches any of the given patterns
     */
    public static boolean simpleMatch(String[] patterns, String str) {
        if (patterns != null) {
            for (int i = 0; i < patterns.length; i++) {
                if (simpleMatch(patterns[i], str)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * String string = "/get/64e967af97ac478f2a919891.jpg";
     * String pattern = "/get/{filePath}";
     * 判断给定的url是否与指定的url模式匹配
     * @param str 要匹配的url
     * @param pattern url模式，可以包含占位符 {@{参数名}}
     * @return 如果url与url模式匹配，则返回true；否则返回false
     */
    public static boolean simpleMatchArgsUrl(String pattern, String str) {
        // 替换url模式中的占位符为通配符 *
        String replacedString = ReUtil.replaceAll(pattern, "\\{\\w+\\}", (match) -> {
            String filePath = match.group(0);
            return "*";
        });

        // 进行匹配
        return PatternMatchUtils.simpleMatch(replacedString, str);
    }

}
