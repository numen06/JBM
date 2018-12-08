package com.jbm.framework.exceptions;

/**
 * 
 * 终止异常
 * 
 * @author Wesley
 * 
 */
public class StopException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StopException() {
		super();
	}

	public StopException(String message, Throwable cause) {
		super(message, cause);
	}

	public StopException(String message) {
		super(message);
	}

	public StopException(Throwable cause) {
		super(cause);
	}

}
