package com.jbm.framework.exceptions.file;

/**
 * 禁止上传的文件扩展名异常类
 *
 * @author wesley.zhang
 */
public class ForbiddenExtensionException extends FileException {
    private static final long serialVersionUID = 1L;

    public ForbiddenExtensionException(String extension, String filename) {
        super("upload.forbidden.extension", new Object[]{extension, filename});
    }
}
