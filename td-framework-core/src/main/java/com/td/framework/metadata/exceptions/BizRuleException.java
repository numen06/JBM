package com.td.framework.metadata.exceptions;

public class BizRuleException extends BizException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4559047934010469927L;

	public BizRuleException(String message, Throwable cause) {
		super(message, cause);
	}

	public BizRuleException(String message) {
		super(message);
	}

	public BizRuleException(Throwable cause) {
		super(cause);
	}
}
