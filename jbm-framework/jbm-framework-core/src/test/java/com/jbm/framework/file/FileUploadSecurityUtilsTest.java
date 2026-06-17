package com.jbm.framework.file;

import com.jbm.framework.exceptions.file.ForbiddenExtensionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadSecurityUtilsTest {

    @Test
    void shouldDenyJspFiles() {
        assertTrue(FileUploadSecurityUtils.isDeniedExtension("test.jsp"));
        assertTrue(FileUploadSecurityUtils.isDeniedExtension("evil.JSP"));
    }

    @Test
    void shouldDenyDoubleExtensionBypass() {
        assertTrue(FileUploadSecurityUtils.isDeniedExtension("a.jsp.jpg"));
    }

    @Test
    void shouldAllowCommonDocumentTypes() {
        assertFalse(FileUploadSecurityUtils.isDeniedExtension("document.pdf"));
        assertFalse(FileUploadSecurityUtils.isDeniedExtension("photo.jpg"));
        assertFalse(FileUploadSecurityUtils.isDeniedExtension("archive.zip"));
        assertFalse(FileUploadSecurityUtils.isDeniedExtension("report.docx"));
    }

    @Test
    void shouldAllowFilesWithoutExtension() {
        assertFalse(FileUploadSecurityUtils.isDeniedExtension("README"));
        assertFalse(FileUploadSecurityUtils.isDeniedExtension(""));
        assertFalse(FileUploadSecurityUtils.isDeniedExtension(null));
    }

    @Test
    void assertAllowedShouldThrowForDeniedExtension() {
        ForbiddenExtensionException exception = assertThrows(
                ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertAllowed("shell.jsp")
        );
        assertTrue(exception.getMessage().contains("upload.forbidden.extension"));
    }

    @Test
    void assertAllowedShouldPassForAllowedExtension() {
        assertDoesNotThrow(() -> FileUploadSecurityUtils.assertAllowed("report.pdf"));
    }
}
