package com.td.util.bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Preconditions;

public class MethodWrap {
	/** Object sporting the subscriber method. */
	private final Object target;
	/** Subscriber method. */
	private final Method method;

	/**
	 * Creates a new MethodWrap to wrap {@code method} on @{code target}.
	 *
	 * @param target
	 *            object to which the method applies.
	 * @param method
	 *            subscriber method.
	 */
	public MethodWrap(Object target, Method method) {
		Preconditions.checkNotNull(target, "MethodWrap target cannot be null.");
		Preconditions.checkNotNull(method, "MethodWrap method cannot be null.");

		this.target = target;
		this.method = method;
		method.setAccessible(true);
	}

	/**
	 * Invokes the wrapped subscriber method to handle {@code event}.
	 *
	 * @param event
	 *            event to handle
	 * @throws InvocationTargetException
	 *             if the wrapped method throws any {@link Throwable} that is
	 *             not an {@link Error} ({@code Error} instances are propagated
	 *             as-is).
	 */
	public void handleEvent(Object event) throws InvocationTargetException {
		try {
			method.invoke(target, new Object[] { event });
		} catch (IllegalArgumentException e) {
			throw new Error("Method rejected target/argument: " + event, e);
		} catch (IllegalAccessException e) {
			throw new Error("Method became inaccessible: " + event, e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			throw e;
		}
	}

	@Override
	public String toString() {
		return "[wrapper " + method + "]";
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		return (PRIME + method.hashCode()) * PRIME + System.identityHashCode(target);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MethodWrap) {
			MethodWrap that = (MethodWrap) obj;
			// Use == so that different equal instances will still receive
			// events.
			// We only guard against the case that the same object is registered
			// multiple times
			return target == that.target && method.equals(that.method);
		}
		return false;
	}

	public Object getTarget() {
		return target;
	}

	public Method getMethod() {
		return method;
	}
}
