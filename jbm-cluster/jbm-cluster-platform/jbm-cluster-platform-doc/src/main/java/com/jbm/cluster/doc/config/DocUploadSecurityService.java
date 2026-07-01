package com.jbm.cluster.doc.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.doc.common.file.UploadCategory;
import com.jbm.framework.file.FileUploadSecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于配置的上传扩展名校验
 */
@Service
public class DocUploadSecurityService {

    private final Set<String> imageAllowedExtensions;
    private final Set<String> documentAllowedExtensions;

    public DocUploadSecurityService(DocUploadSecurityProperties properties) {
        this.imageAllowedExtensions = resolveExtensions(
                properties.getImageAllowedExtensions(),
                properties.getAdditionalImageExtensions(),
                FileUploadSecurityUtils.IMAGE_ALLOWED_EXTENSIONS);
        this.documentAllowedExtensions = resolveExtensions(
                properties.getDocumentAllowedExtensions(),
                properties.getAdditionalDocumentExtensions(),
                FileUploadSecurityUtils.DOCUMENT_ALLOWED_EXTENSIONS);
    }

    public void assertAllowed(String filename, UploadCategory category) {
        if (category == UploadCategory.IMAGE) {
            assertImageAllowed(filename);
        } else {
            assertDocumentAllowed(filename);
        }
    }

    public void assertImageAllowed(String filename) {
        FileUploadSecurityUtils.assertAllowedInWhitelist(filename, imageAllowedExtensions);
    }

    public void assertDocumentAllowed(String filename) {
        FileUploadSecurityUtils.assertAllowedInWhitelist(filename, documentAllowedExtensions);
    }

    public Set<String> getImageAllowedExtensions() {
        return imageAllowedExtensions;
    }

    public Set<String> getDocumentAllowedExtensions() {
        return documentAllowedExtensions;
    }

    private static Set<String> resolveExtensions(List<String> overrideList,
                                                 List<String> additionalList,
                                                 Set<String> defaults) {
        Set<String> resolved = new HashSet<>();
        if (CollUtil.isNotEmpty(overrideList)) {
            resolved.addAll(normalize(overrideList));
        } else {
            resolved.addAll(defaults);
            resolved.addAll(normalize(additionalList));
        }
        return Collections.unmodifiableSet(resolved);
    }

    private static Set<String> normalize(List<String> extensions) {
        if (CollUtil.isEmpty(extensions)) {
            return Collections.emptySet();
        }
        return extensions.stream()
                .filter(StrUtil::isNotBlank)
                .map(DocUploadSecurityService::normalizeExtension)
                .collect(Collectors.toSet());
    }

    private static String normalizeExtension(String extension) {
        return StrUtil.removePrefix(extension.trim().toLowerCase(), ".");
    }
}
