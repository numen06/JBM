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

    /**
     * 图片上传默认白名单扩展名
     */
    public static final Set<String> IMAGE_ALLOWED_EXTENSIONS;

    /**
     * 文档上传默认白名单扩展名（对齐 WebFileUtil 办公类型）
     */
    public static final Set<String> DOCUMENT_ALLOWED_EXTENSIONS;

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

        IMAGE_ALLOWED_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico"
        )));

        Set<String> documentExtensions = new HashSet<>();
        // word
        documentExtensions.addAll(Arrays.asList(
                "doc", "docx", "txt", "dot", "wps", "wpt", "dotx", "docm", "dotm"
        ));
        // excel
        documentExtensions.addAll(Arrays.asList(
                "xls", "xlsx", "xlt", "xlsm", "xltx", "xltm", "csv", "et"
        ));
        // ppt
        documentExtensions.addAll(Arrays.asList(
                "ppt", "pptx", "pptm", "ppsm", "pps", "potx", "potm", "dpt", "dps"
        ));
        // pdf
        documentExtensions.add("pdf");
        DOCUMENT_ALLOWED_EXTENSIONS = Collections.unmodifiableSet(documentExtensions);
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
     * 校验文件名（黑名单），不通过则抛出 {@link ForbiddenExtensionException}
     *
     * @param filename 文件名
     */
    public static void assertAllowed(String filename) {
        String deniedExtension = findDeniedExtension(filename);
        if (deniedExtension != null) {
            throw new ForbiddenExtensionException(deniedExtension, filename);
        }
    }

    /**
     * 校验图片扩展名白名单
     *
     * @param filename 文件名
     */
    public static void assertImageAllowed(String filename) {
        assertAllowedInWhitelist(filename, IMAGE_ALLOWED_EXTENSIONS);
    }

    /**
     * 校验文档扩展名白名单
     *
     * @param filename 文件名
     */
    public static void assertDocumentAllowed(String filename) {
        assertAllowedInWhitelist(filename, DOCUMENT_ALLOWED_EXTENSIONS);
    }

    /**
     * 校验文件名扩展名是否在白名单内，并叠加黑名单兜底
     *
     * @param filename           文件名
     * @param allowedExtensions  允许的扩展名集合（小写）
     */
    public static void assertAllowedInWhitelist(String filename, Set<String> allowedExtensions) {
        String deniedExtension = findDeniedExtension(filename);
        if (deniedExtension != null) {
            throw new ForbiddenExtensionException(deniedExtension, filename);
        }
        String[] segments = getExtensionSegments(filename);
        if (segments == null || segments.length == 0) {
            throw new ForbiddenExtensionException("", StrUtil.nullToEmpty(filename));
        }
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i].toLowerCase();
            if (!allowedExtensions.contains(segment)) {
                throw new ForbiddenExtensionException(segment, filename);
            }
        }
        String finalExtension = segments[segments.length - 1].toLowerCase();
        if (!allowedExtensions.contains(finalExtension)) {
            throw new ForbiddenExtensionException(finalExtension, filename);
        }
    }

    private static String findDeniedExtension(String filename) {
        String[] segments = getExtensionSegments(filename);
        if (segments == null) {
            return null;
        }
        for (String segment : segments) {
            if (StrUtil.isNotBlank(segment) && DENIED_EXTENSIONS.contains(segment.toLowerCase())) {
                return segment;
            }
        }
        return null;
    }

    private static String[] getExtensionSegments(String filename) {
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
        if (segments.length < 2) {
            return null;
        }
        String[] extensionSegments = new String[segments.length - 1];
        System.arraycopy(segments, 1, extensionSegments, 0, extensionSegments.length);
        return extensionSegments;
    }
}
