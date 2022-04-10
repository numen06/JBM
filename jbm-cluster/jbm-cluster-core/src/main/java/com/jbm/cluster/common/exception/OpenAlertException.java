package com.jbm.cluster.common.exception;

import com.jbm.framework.exceptions.ServiceException;

/**
 * 提示消息异常
 *
 * @author wesley
 */
public class OpenAlertException extends ServiceException {
    private static final long serialVersionUID = 4908906410210213271L;

    public OpenAlertException() {
    }

    public OpenAlertException(String msg) {
        super(msg);
    }

    public OpenAlertException(int code, String msg) {
        super(code, msg);
    }

    public OpenAlertException(int code, String msg, Throwable cause) {
        super(code, msg, cause);
    }
}
