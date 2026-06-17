package com.jbm.framework.file;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.exceptions.file.ForbiddenExtensionException;
import com.jbm.util.FileNameUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件上传安全校验工具类
 *
 * @author wesley.zhang
 */
public final class FileUploadSecurityUtils {

    private static final Set<String> DENIED_EXTENSIONS;

    static {
        Set<String> extensions = new HashSet<>();
        // Web 脚本
        extensions.addAll(Arrays.asList(
                "jsp", "jspx", "jspf", "asp", "aspx", "ashx", "asmx", "php", "phtml", "phar", "cgi"
        ));
        // 可执行/脚本
        extensions.addAll(Arrays.asList(
                "exe", "dll", "bat", "cmd", "com", "scr", "msi", "sh", "bash", "ps1", "vbs", "vbe", "js"
        ));
        // Java Web 包
        extensions.addAll(Arrays.asList("war", "jar", "class"));
        // 其他
        extensions.addAll(Arrays.asList("htaccess", "htpasswd"));
        DENIED_EXTENSIONS = Collections.unmodifiableSet(extensions);
    }

    private FileUploadSecurityUtils() {
    }

    /**
     * 判断文件名是否包含被拒绝的扩展名
     *
     * @param filename 文件名
     * @return 是否被拒绝
     */
    public static boolean isDeniedExtension(String filename) {
        return findDeniedExtension(filename) != null;
    }

    /**
     * 校验文件名，不通过则抛出 {@link ForbiddenExtensionException}
     *
     * @param filename 文件名
     */
    public static void assertAllowed(String filename) {
        String deniedExtension = findDeniedExtension(filename);
        if (deniedExtension != null) {
            throw new ForbiddenExtensionException(deniedExtension, filename);
        }
    }

    private static String findDeniedExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return null;
        }
        String name = FileNameUtils.getName(filename);
        if (StrUtil.isBlank(name)) {
            return null;
        }
        int dotIndex = name.indexOf('.');
        if (dotIndex == -1) {
            return null;
        }
        String[] segments = name.split("\\.");
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            if (StrUtil.isNotBlank(segment) && DENIED_EXTENSIONS.contains(segment.toLowerCase())) {
                return segment;
            }
        }
        return null;
    }
}
