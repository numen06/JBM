package com.jbm.framework.exceptions;

import com.jbm.framework.metadata.enumerate.ErrorCode;

/**
 * 业务错误基础类
 *
 * @author wesley
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code = ErrorCode.ERROR.getCode();

    public ServiceException() {
    }

    public ServiceException(Exception e) {
        super(e);
    }

    public ServiceException(String msg) {
        super(msg);
    }

    public ServiceException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public ServiceException(String msg, Exception e) {
        super(msg, e);
    }

    public ServiceException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
