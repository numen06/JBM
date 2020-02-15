package com.jbm.cluster.common.exception;

import com.jbm.framework.exceptions.ServiceException;

/**
 * 签名异常
 *
 * @author admin
 */
public class OpenSignatureException extends ServiceException {
    private static final long serialVersionUID = 1L;

    public OpenSignatureException() {
    }

    public OpenSignatureException(String msg) {
        super(msg);
    }

    public OpenSignatureException(int code, String msg) {
        super(code, msg);
    }

    public OpenSignatureException(int code, String msg, Throwable cause) {
        super(code, msg, cause);
    }
}
