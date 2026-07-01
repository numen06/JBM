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

    @Test
    void imageWhitelistShouldAllowCommonImageTypes() {
        assertDoesNotThrow(() -> FileUploadSecurityUtils.assertImageAllowed("photo.jpg"));
        assertDoesNotThrow(() -> FileUploadSecurityUtils.assertImageAllowed("photo.PNG"));
    }

    @Test
    void imageWhitelistShouldRejectNonImageTypes() {
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertImageAllowed("report.pdf"));
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertImageAllowed("archive.zip"));
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertImageAllowed("shell.jsp"));
    }

    @Test
    void documentWhitelistShouldAllowOfficeTypes() {
        assertDoesNotThrow(() -> FileUploadSecurityUtils.assertDocumentAllowed("report.pdf"));
        assertDoesNotThrow(() -> FileUploadSecurityUtils.assertDocumentAllowed("report.docx"));
        assertDoesNotThrow(() -> FileUploadSecurityUtils.assertDocumentAllowed("sheet.xlsx"));
    }

    @Test
    void documentWhitelistShouldRejectImageAndArchiveTypes() {
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertDocumentAllowed("photo.jpg"));
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertDocumentAllowed("archive.zip"));
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertDocumentAllowed("shell.jsp"));
    }

    @Test
    void whitelistShouldRejectDoubleExtensionBypass() {
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertImageAllowed("a.jsp.jpg"));
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertDocumentAllowed("a.jsp.pdf"));
    }

    @Test
    void whitelistShouldRejectFilesWithoutExtension() {
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertImageAllowed("README"));
        assertThrows(ForbiddenExtensionException.class,
                () -> FileUploadSecurityUtils.assertDocumentAllowed(""));
    }
}
