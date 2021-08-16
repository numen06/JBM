package com.jbm.framework.cloud.auth.exception;

/**
 * @author wesley
 * @date 2017-12-29 17:05:10
 * 403 授权拒绝
 */
public class JbmDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JbmDeniedException() {
    }

    public JbmDeniedException(String message) {
        super(message);
    }

    public JbmDeniedException(Throwable cause) {
        super(cause);
    }

    public JbmDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public JbmDeniedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
