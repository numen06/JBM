package com.jbm.cluster.doc.config;

import com.jbm.framework.exceptions.file.ForbiddenExtensionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocUploadSecurityServiceTest {

    @Test
    void shouldMergeAdditionalDocumentExtensionsWithDefaults() {
        DocUploadSecurityProperties properties = new DocUploadSecurityProperties();
        properties.setAdditionalDocumentExtensions(Arrays.asList("log", "zip"));
        DocUploadSecurityService service = new DocUploadSecurityService(properties);

        assertDoesNotThrow(() -> service.assertDocumentAllowed("report.pdf"));
        assertDoesNotThrow(() -> service.assertDocumentAllowed("app.log"));
        assertDoesNotThrow(() -> service.assertDocumentAllowed("bundle.zip"));
        assertThrows(ForbiddenExtensionException.class, () -> service.assertDocumentAllowed("photo.jpg"));
    }

    @Test
    void shouldOverrideDocumentExtensionsWhenConfigured() {
        DocUploadSecurityProperties properties = new DocUploadSecurityProperties();
        properties.setDocumentAllowedExtensions(Arrays.asList("log"));
        DocUploadSecurityService service = new DocUploadSecurityService(properties);

        assertDoesNotThrow(() -> service.assertDocumentAllowed("app.log"));
        assertThrows(ForbiddenExtensionException.class, () -> service.assertDocumentAllowed("report.pdf"));
    }

    @Test
    void shouldMergeAdditionalImageExtensionsWithDefaults() {
        DocUploadSecurityProperties properties = new DocUploadSecurityProperties();
        properties.setAdditionalImageExtensions(Arrays.asList("svg", "tiff"));
        DocUploadSecurityService service = new DocUploadSecurityService(properties);

        assertDoesNotThrow(() -> service.assertImageAllowed("photo.jpg"));
        assertDoesNotThrow(() -> service.assertImageAllowed("icon.svg"));
        assertDoesNotThrow(() -> service.assertImageAllowed("scan.tiff"));
        assertThrows(ForbiddenExtensionException.class, () -> service.assertImageAllowed("report.pdf"));
    }

    @Test
    void shouldOverrideImageExtensionsWhenConfigured() {
        DocUploadSecurityProperties properties = new DocUploadSecurityProperties();
        properties.setImageAllowedExtensions(Arrays.asList("webp"));
        DocUploadSecurityService service = new DocUploadSecurityService(properties);

        assertDoesNotThrow(() -> service.assertImageAllowed("photo.webp"));
        assertThrows(ForbiddenExtensionException.class, () -> service.assertImageAllowed("photo.jpg"));
    }
}
