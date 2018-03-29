package com.td.framework.metadata.exceptions.http;

/**
 * Http连接错误
 * 
 * @author wesley
 *
 */
public class HttpConnectionException extends Exception {
	private static final long serialVersionUID = -2615370590441195647L;
	private boolean readTimedout = false;
	private int doneRetriedTimes = 0;

	public HttpConnectionException(String message, Throwable e) {
		super(message, e);
	}

	public HttpConnectionException(String message, Throwable e, int doneRetriedTimes) {
		super(message, e);
		this.doneRetriedTimes = doneRetriedTimes;
	}

	public HttpConnectionException(String message, Throwable e, boolean readTimedout) {
		super(message, e);
		this.readTimedout = readTimedout;
	}

	public boolean isReadTimedout() {
		return readTimedout;
	}

	public int getDoneRetriedTimes() {
		return this.doneRetriedTimes;
	}
}
