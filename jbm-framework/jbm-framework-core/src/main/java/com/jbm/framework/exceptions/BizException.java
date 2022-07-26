package com.jbm.framework.exceptions;

/**
 * @author wesley
 */
public class BizException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 9124123550084743362L;

    private Integer errId;

    public BizException() {
        super();
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }

    public BizException(Integer errId, String message, Throwable cause) {
        super(message, cause);
        this.errId = errId;
    }

    public BizException(ServiceException serviceException) {
        this(serviceException.getCode(), serviceException.getMessage(), serviceException.getCause());
    }

    public BizException(String message) {
        super(message);
    }

    public BizException(Throwable cause) {
        super(cause);
    }

    public Integer getErrId() {
        return errId;
    }

    public void setErrId(Integer errId) {
        this.errId = errId;
    }
}
