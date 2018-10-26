package com.jbm.framework.metadata.usage.bean;

import java.util.concurrent.TimeUnit;

public interface IFuture {
	/**
	 * Wait for the asynchronous operation to complete. The attached listeners
	 * will be notified when the operation is completed.
	 */
	IFuture await() throws InterruptedException;

	/**
	 * Wait for the asynchronous operation to complete with the specified
	 * timeout.
	 *
	 * @return <tt>true</tt> if the operation is completed.
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * Wait for the asynchronous operation to complete with the specified
	 * timeout.
	 *
	 * @return <tt>true</tt> if the operation is completed.
	 */
	boolean await(long timeoutMillis) throws InterruptedException;

	/**
	 * Returns if the asynchronous operation is completed.
	 */
	boolean isDone();

}
