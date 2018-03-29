package org.springframework.data.level.core;

public interface TimeToLiveAccessor {
	/**
	 * @param source
	 *            must not be {@literal null}.
	 * @return {@literal null} if not configured.
	 */
	Long getTimeToLive(Object source);
}
