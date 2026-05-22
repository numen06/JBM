package com.jbm.framework.exceptions;

import com.jbm.framework.metadata.enumerate.ErrorCode;

/**
 * API 签名校验失败
 */
public class OpenSignatureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public OpenSignatureException(String message) {
        super(message);
        this.code = ErrorCode.SIGNATURE_DENIED.getCode();
    }

    public OpenSignatureException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.SIGNATURE_DENIED.getCode();
    }

    public int getCode() {
        return code;
    }
}
