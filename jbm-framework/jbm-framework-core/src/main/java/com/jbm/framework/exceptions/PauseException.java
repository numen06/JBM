package com.jbm.framework.exceptions;

/**
 * 暂停异常
 *
 * @author Wesley
 */
public class PauseException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PauseException() {
        super();
    }

    public PauseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PauseException(String message) {
        super(message);
    }

    public PauseException(Throwable cause) {
        super(cause);
    }

}
